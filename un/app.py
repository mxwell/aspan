import argparse
import logging
from logging.config import dictConfig
import os
from os.path import join as pj
import sys
import threading

from flask import Flask, jsonify, request, make_response, send_file
from speechkit import model_repository, configure_credentials, creds
import sqlite3

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
app = Flask("un_app")
unInstance = None


def read_token(path):
    return open(path).read().strip()


class Un(object):

    def __init__(self, audio_workdir, yc_api_key, db_conn):
        self.audio_workdir = audio_workdir
        self.counter = 101

        while True:
            path = self.make_filepath_for_counter(self.counter)
            if not os.path.exists(path):
                break
            self.counter += 1

        logging.info("Counter initialized to %d", self.counter)

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
        self.model.voice = "madi"

        self.soft_model = model_repository.synthesis_model()
        self.soft_model.voice = "amira"

        self.db_lock = threading.Lock()
        self.db_conn = db_conn

    def make_filepath_for_counter(self, counter):
        name = "gen_{:03d}".format(counter)
        path_prefix = pj(self.audio_workdir, name)
        return "{}.mp3".format(path_prefix)

    def make_filepath(self):
        cur = self.counter
        self.counter += 1
        return self.make_filepath_for_counter(cur)

    def check_verb_and_get_soft(self, verb, fe):
        with self.db_lock:
            cursor = self.db_conn.cursor()
            query = "SELECT soft FROM Verbs WHERE verb = ? AND fe = ?"
            cursor.execute(query, (verb, fe))
            result = cursor.fetchone()
            if result is not None:
                return True, result[0]
            else:
                return False, None

    def get_model(self, soft):
        if soft:
            return self.soft_model
        else:
            return self.model

    def generate(self, verb, fe, text):
        present, soft = self.check_verb_and_get_soft(verb, fe)
        if not present:
            logging.error("Unknown verb: %s, %s", verb, str(fe))
            return None
        path = self.make_filepath()
        result = self.get_model(soft).synthesize(text, raw_format=False)
        result.export(path, format="mp3")
        logging.info("Generated audio for %s: %s", text, path)
        return path


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

    logging.info("Database connection with %s established", db_path)
    return conn


def init_un_app():
    global unInstance
    db_conn = init_db_conn(DATABASE_PATH)
    unInstance = Un("audio_workdir", ".secrets/.yc.apikey", db_conn)
    logging.info("Un app initialized")


@app.route("/api/v1/test", methods=["GET"])
def get_test():
    return jsonify({"message": "You've reached Un!"}), 200


@app.route("/api/v1/tts", methods=["GET"])
def get_sound():
    global unInstance

    verb = request.args.get("v")
    fe = request.args.get("fe") == "1"
    form = request.args.get("f")

    logging.info("Request: [%s]%s -> [%s]",
        verb,
        " forced exceptional" if (fe) else "",
        form,
    )

    if not verb:
        logging.error("Invalid request: no verb")
        return jsonify({"message": "Invalid request"}), 400
    if not form:
        logging.error("Invalid request: no form")
        return jsonify({"message": "Invalid request"}), 400

    path = unInstance.generate(verb, fe, form)
    if not path:
        logging.error("Failed to generate audio")
        return jsonify({"message": "Invalid request"}), 400
    return send_file(path, as_attachment=False)


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
