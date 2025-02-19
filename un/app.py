import argparse
import json
import logging
from logging.config import dictConfig
import os
from os.path import join as pj
import requests
import sqlite3
import sys
import threading
import urllib.parse
import uuid

import boto3
from flask import Flask, jsonify, redirect, request, make_response, send_file
from speechkit import model_repository, configure_credentials, creds

from lib.limiter import Limiter
from lib.translit import transliterate, check_content

dictConfig({
    'version': 1,
    'formatters': {'default': {
        'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
    }},
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'default'
    }},
    'root': {
        'level': 'INFO',
        'handlers': ['wsgi']
    }
})
DATABASE_PATH = "un.db"
BUCKET_NAME="verbforms"
BUCKET_URL = f"https://storage.yandexcloud.net/{BUCKET_NAME}/"
app = Flask("un_app")
unInstance = None

ALLOWED_NONTEXT = " .,!?-():;\"'"


def read_token(path):
    return open(path).read().strip()


class Synth(object):

    def synthesize(self, soft, text, path, name):
        result = self.get_model(soft).synthesize(text, raw_format=False)
        result.export(path, format="mp3")
        logging.info("Generated audio for %s: %s", text, name)

    def get_voice(self, soft):
        if soft:
            return "amira"
        return "madi"


class SynthYskV1(Synth):

    def __init__(self, yc_api_key, yc_folder_id):
        self.headers = {
            "Authorization": f"Api-Key {yc_api_key}",
        }
        self.yc_folder_id = yc_folder_id
        self.url = "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize"

    def request(self, soft, text):
        data = {
            "text": text,
            "lang": "kk-KK",
            "voice": self.get_voice(soft),
            "format": "mp3",
            "folderId": self.yc_folder_id,
        }
        with requests.post(self.url, headers=self.headers, data=data, stream=True) as resp:
            if resp.status_code != 200:
                raise RuntimeError("Invalid response received: code: %d, message: %s" % (resp.status_code, resp.text))

            for chunk in resp.iter_content(chunk_size=None):
                yield chunk

    def generate_audio(self, soft, text, path, name):
        total = 0
        with open(path, "wb") as output_file:
            for audio_content in self.request(soft, text):
                output_file.write(audio_content)
                total += len(audio_content)
        logging.info("Generated audio of %d for %s: %s", total, text, name)

    def generate_sentence_audio(self, voice_id, sentence, path, name):
        soft = voice_id > 0
        total = 0
        with open(path, "wb") as output_file:
            for audio_content in self.request(soft, sentence):
                output_file.write(audio_content)
                total += len(audio_content)
        logging.info("Generated sentence audio of %d for %s: %s", total, sentence, name)


class SynthYskV3(Synth):

    def __init__(self, yc_api_key):
        logging.info("YSK: configuring...")
        assert isinstance(yc_api_key, str), f"Invalid type: {type(yc_api_key)}"

        yc_api_key_text = read_token(yc_api_key)
        configure_credentials(
            yandex_credentials=creds.YandexCredentials(
                api_key=yc_api_key_text,
            )
        )
        logging.info("YSK: configured credentials")

        self.model = model_repository.synthesis_model()
        self.model.voice = self.get_voice(False)

        self.soft_model = model_repository.synthesis_model()
        self.soft_model.voice = self.get_voice(True)

    def get_model(self, soft):
        if soft:
            return self.soft_model
        return self.model

    def generate_audio(self, soft, text, path, name):
        result = self.get_model(soft).synthesize(text, raw_format=False)
        result.export(path, format="mp3")
        logging.info("Generated audio for %s: %s", text, name)

    def generate_sentence_audio(self, voice_id, sentence, path, name):
        soft = voice_id > 0
        result = self.get_model(soft).synthesize(sentence, raw_format=False)
        result.export(path, format="mp3")
        logging.info("Generated audio for %s: %s", sentence, name)


class Un(object):

    def __init__(self, audio_workdir, synth, db_conn):
        self.audio_workdir = audio_workdir
        assert isinstance(synth, Synth)
        self.synth = synth
        self.limiter = Limiter(60)

        self.boto_session = boto3.session.Session()
        self.s3 = self.boto_session.client(
            service_name="s3",
            endpoint_url="https://storage.yandexcloud.net"
        )

        self.db_lock = threading.Lock()
        self.db_conn = db_conn

    def make_audio_name(self, verb, fe, text):
        verbT = transliterate(verb)
        if not verbT:
            return None
        textT = transliterate(text)
        if not textT:
            return None
        suffix = str(uuid.uuid4())[:6]
        return f"{verbT}{int(fe)}{textT}_{suffix}"

    def make_sentence_audio_name(self, sentence):
        sentenceT = transliterate(sentence[:32].strip())
        if not sentenceT:
            return None
        suffix = str(uuid.uuid4())[:6]
        return f"sent_{sentenceT}_{suffix}"

    # returns `(id: int, soft: boolean)` or `None, None`
    def check_verb_and_get_soft(self, verb, fe):
        with self.db_lock:
            cursor = self.db_conn.cursor()
            query = "SELECT id, soft FROM Verbs WHERE verb = ? AND fe = ?"
            cursor.execute(query, (verb, fe))
            result = cursor.fetchone()
            if result is not None:
                return result[0], result[1]
            else:
                return None, None

    # returns `audio_name: string` or `None`
    def check_text_audio(self, verb_id, text):
        with self.db_lock:
            cursor = self.db_conn.cursor()
            query = "SELECT audio FROM Audio WHERE verb_id = ? AND text = ?"
            cursor.execute(query, (verb_id, text))
            result = cursor.fetchone()
            if result is not None:
                return result[0]
            else:
                return None

    # returns `audio_name: string` or `None`
    def check_sentence_audio(self, sentence):
        with self.db_lock:
            cursor = self.db_conn.cursor()
            query = "SELECT audio FROM SentenceAudio WHERE sentence = ?"
            cursor.execute(query, (sentence,))
            result = cursor.fetchone()
            if result is not None:
                return result[0]
            else:
                return None

    def upload_audio_to_s3(self, audio_path, audio_name):
        self.s3.upload_file(audio_path, BUCKET_NAME, f"{audio_name}.mp3")
        logging.info("Uploaded audio %s to S3", audio_name)

    def store_text_audio_to_db(self, verb_id, text, audio_name):
        with self.db_lock:
            cursor = self.db_conn.cursor()
            insert_query = "INSERT INTO Audio (verb_id, text, audio) VALUES (?, ?, ?)"""
            cursor.execute(insert_query, (verb_id, text, audio_name))
            self.db_conn.commit()
            logging.info("Stored to db: %d, %s, %s", verb_id, text, audio_name)

    def store_sentence_audio_to_db(self, sentence, audio_name):
        with self.db_lock:
            cursor = self.db_conn.cursor()
            insert_query = "INSERT INTO SentenceAudio (sentence, audio) VALUES (?, ?)"""
            cursor.execute(insert_query, (sentence, audio_name))
            self.db_conn.commit()
            logging.info("Stored to db: %s, %s", sentence, audio_name)

    def make_audio_path(self, name):
        return pj(self.audio_workdir, f"{name}.mp3")

    def make_audio_url(self, name):
        audio_url = f"{BUCKET_URL}{name}.mp3"
        return audio_url

    def acquire(self):
        if not self.limiter.acquire():
            logging.info("Audio generation was rate limited")
            return False
        return True

    def get_audio_file(self, verb, fe, verb_id, text, soft):
        existing_audio_name = self.check_text_audio(verb_id, text)
        if existing_audio_name:
            logging.info("Audio is found in DB: %s", existing_audio_name)
            return self.make_audio_path(existing_audio_name)
        name = self.make_audio_name(verb, fe, text)
        if not name:
            return None
        path = self.make_audio_path(name)
        if not self.acquire():
            return None
        self.synth.generate_audio(soft, text, path, name)
        self.upload_audio_to_s3(path, name)
        self.store_text_audio_to_db(verb_id, text, name)
        return path

    # returns a path to a local file or `None`
    def generate(self, verb, fe, text):
        verb_id, soft = self.check_verb_and_get_soft(verb, fe)
        if not verb_id:
            logging.error("Unknown verb: %s, %s", verb, str(fe))
            return None
        return self.get_audio_file(verb, fe, verb_id, text, soft)

    # returns an URL to a remote file or `None`, and a status code
    def generate_audio_url(self, verb, fe, text):
        verb_id, soft = self.check_verb_and_get_soft(verb, fe)
        if not verb_id:
            logging.error("Unknown verb: %s, %s", verb, str(fe))
            return None, 400
        existing_audio_name = self.check_text_audio(verb_id, text)
        if existing_audio_name:
            logging.info("Audio is found in DB: %s", existing_audio_name)
            return self.make_audio_url(existing_audio_name), 200
        name = self.make_audio_name(verb, fe, text)
        if not name:
            return None, 400
        path = self.make_audio_path(name)
        if not self.acquire():
            return None, 429
        self.synth.generate_audio(soft, text, path, name)
        self.upload_audio_to_s3(path, name)
        self.store_text_audio_to_db(verb_id, text, name)
        os.unlink(path)
        return self.make_audio_url(name), 201

    # returns an URL to a remote file or `None`, and a status code
    def generate_sentence_audio_url(self, sentence, voice_id):
        existing_audio_name = self.check_sentence_audio(sentence)
        if existing_audio_name:
            logging.info("Sentence audio is found in DB: %s", existing_audio_name)
            return self.make_audio_url(existing_audio_name), 200
        name = self.make_sentence_audio_name(sentence)
        if not name:
            return None, 400
        path = self.make_audio_path(name)
        if not self.acquire():
            return None, 429
        self.synth.generate_sentence_audio(voice_id, sentence, path, name)
        self.upload_audio_to_s3(path, name)
        self.store_sentence_audio_to_db(sentence, name)
        os.unlink(path)
        return self.make_audio_url(name), 201


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("""
CREATE TABLE IF NOT EXISTS Verbs (
    id INTEGER PRIMARY KEY,
    verb TEXT NOT NULL,
    fe BOOLEAN NOT NULL,
    soft BOOLEAN NOT NULL
);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_verb ON Verbs (verb, fe);
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS Audio (
    verb_id INTEGER NOT NULL,
    text TEXT NOT NULL,
    audio TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (verb_id, text)
);
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS SentenceAudio (
    id INTEGER PRIMARY KEY,
    sentence TEXT NOT NULL,
    audio TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_sentence_audio ON SentenceAudio (sentence);
    """.strip())

    logging.info("Database connection with %s established", db_path)
    return conn


def init_un_app():
    global unInstance
    db_conn = init_db_conn(DATABASE_PATH)
    yc_api_key_path = ".secrets/.yc.apikey"
    yc_api_key = read_token(yc_api_key_path)
    yc_folder_id = read_token(".secrets/.yc.folderid")
    # synth = SynthYskV3(yc_api_key_path)
    synth = SynthYskV1(yc_api_key, yc_folder_id)
    unInstance = Un("audio_workdir", synth, db_conn)
    logging.info("Un app initialized")


# Returns:
# - (breakdown, None), if the sentence is a valid Kazakh text, where breakdown is a dict with details
# - (None, error_message), otherwise
def request_grammar_breakdown(s):
    encoded = urllib.parse.quote(s)
    kiltman_url = f"https://kazakhverb.khairulin.com/analyze?q={encoded}"
    response = requests.get(kiltman_url)
    if response.status_code != 200:
        logging.error("request_grammar_breakdown: bad status %d", response.status_code)
        return (None, "server error")
    try:
        breakdown = response.json()
    except json.JSONDecodeError as e:
        logging.error("request_grammar_breakdown: bad json in response: %s", str(e))
        return (None, "server error")

    if "parts" not in breakdown:
        logging.error("request_grammar_breakdown: no parts in response: %s", str(j))
        return (None, "server error")

    parts = breakdown["parts"]
    not_recognized = []
    for part in parts:
        if "forms" not in part:
            logging.error("request_grammar_breakdown: no forms in part: %s", str(part))
            return (None, "server error")
        # If there are any recognized forms, then the part is a valid Kazakh text
        if len(part["forms"]) > 0:
            continue
        if "text" not in part:
            logging.error("request_grammar_breakdown: no text in part: %s", str(part))
            return (None, "server error")
        text = part["text"]
        allowed, bad_ch = check_content(text)
        if not allowed:
            if bad_ch == "\n":
                logging.error("request_grammar_breakdown: found end of line in [%s]", s)
                return (None, f"Текст содержит перевод строки. Если текст состоит из нескольких строк, то отправьте эти строки по отдельности в разных сообщениях.")
            logging.error("request_grammar_breakdown: bad symbol in text [%s]: [%s]", text, bad_ch)
            return (None, f"word {text} is unknown, first bad symbol is {bad_ch}")
        nontext = True
        for ch in text:
            if ch not in ALLOWED_NONTEXT:
                nontext = False
                break
        if not nontext:
            not_recognized.append(text)

    not_recognized_length = sum([len(item) for item in not_recognized])
    if not_recognized_length > 0:
        logging.info("request_grammar_breakdown: not recognized %d out of %d", not_recognized_length, len(s))
        # Threshold is 50%
        if not_recognized_length * 2 > len(s) or len(not_recognized) > 10:
            summary = ", ".join(not_recognized)
            return (None, f"Слишком много нераспознанных слов: {summary}")

    return (breakdown, None)


@app.route("/api/v1/test", methods=["GET"])
def get_test():
    global unInstance
    for i in range(10):
        unInstance.acquire()
    return jsonify({"message": "You've reached Un!"}), 200


@app.route("/api/v1/tts", methods=["GET"])
def get_sound():
    global unInstance

    verb = request.args.get("v")
    fe = request.args.get("fe") == "1"
    form = request.args.get("f")

    logging.info("/api/v1/tts: [%s]%s -> [%s]",
        verb,
        " forced exceptional" if (fe) else "",
        form,
    )

    if not verb:
        logging.error("Invalid request: no verb")
        return jsonify({"message": "Invalid request"}), 400
    if len(verb) > 64:
        logging.error("Invalid request: verb length %d", len(verb))
    if not form:
        logging.error("Invalid request: no form")
        return jsonify({"message": "Invalid request"}), 400
    if len(form) > 128:
        logging.error("Invalid request: form length %d", len(form))
        return jsonify({"message": "Invalid request"}), 400

    url, status_code = unInstance.generate_audio_url(verb, fe, form)
    if not url:
        logging.error("Failed to generate audio")
        return jsonify({"message": "Invalid request"}), status_code
    return redirect(url, code=302)


@app.route("/api/v1/sentence_tts", methods=["GET"])
def sentence_tts():
    global unInstance

    s = request.args.get("s")
    voice_value = request.args.get("voice", "1")

    if not voice_value.isdigit():
        logging.error("sentence_tts: bad voice %s", voice_value)
        return jsonify({"message": "Invalid request"}), 400
    voice_id = int(voice_value)
    logging.info("sentence_tts: %d, [%s]", voice_id, s)

    if not s:
        logging.error("sentence_tts: no sentence")
        return jsonify({"message": "Invalid request"}), 400
    if len(s) > 4096:
        logging.error("sentence_tts: sentence too long - %d", len(s))
        return jsonify({"message": "Invalid request"}), 400

    breakdown, error_message = request_grammar_breakdown(s)

    if breakdown is None:
        logging.error("sentence_tts: breakdown error %s", error_message)
        return jsonify({"message": error_message}), 422

    lowered = s.strip().lower()
    url, status_code = unInstance.generate_sentence_audio_url(lowered, voice_id)
    if not url:
        logging.error("Failed to generate audio")
        return jsonify({"message": "Invalid request"}), status_code
    return redirect(url, code=302)


def parse_boolean(part, name, line):
    if part == "0":
        return False
    elif part == "1":
        return True
    else:
        raise ValueError(f"unexpected {name} in input line: {line}")


def load_verbs(args):
    conn = init_db_conn(args.db_path)
    cursor = conn.cursor()
    insert_query = """INSERT INTO Verbs (verb, fe, soft) VALUES (?, ?, ?)"""
    batch = 0
    total = 0
    for line in open(args.verbs_path):
        parts = line.strip().split("\t")
        if len(parts) != 3:
            raise ValueError(f"unexpected number of parts in input line: {len(parts)} != 3, {line}")
        verb = parts[0]
        fe = parse_boolean(parts[1], "fe", line)
        soft = parse_boolean(parts[2], "soft", line)
        cursor.execute(insert_query, (verb, fe, soft))
        batch += 1
        total += 1
        if batch >= 100:
            conn.commit()
            batch = 0
    conn.commit()
    conn.close()
    logging.info("Loaded %d verbs", total)


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    load_verbs_parser = subparsers.add_parser("load-verbs")
    load_verbs_parser.add_argument("--verbs-path", required=True)
    load_verbs_parser.add_argument("--db-path", default=DATABASE_PATH)
    load_verbs_parser.set_defaults(func=load_verbs)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())
else:
    init_un_app()
