import argparse
import json
import logging
import os
import requests
import sqlite3
import sys


ENDPOINT = "transcribe.api.cloud.yandex.net/speech/stt/v2/longRunningRecognize"
STATUS_ENDPOINT = "operation.api.cloud.yandex.net"
DB_PATH = "gc.db"
TABLE_NAME = "subtitles"


def build_body_json(audio_url):
    body = {
        "config": {
            "specification": {
                "languageCode": "kk-KZ",
                "model": "deferred-general",
                "profanityFilter": False,
                "literature_text": True,
                "audioEncoding": "MP3",
                "audioChannelCount": 2,
                "rawResults": True
            }
        },
        "audio": {
            "uri": audio_url
        }
    }
    return body


SAVED_IAM_TOKEN = None


def get_iam_token():
    global SAVED_IAM_TOKEN

    if SAVED_IAM_TOKEN is None:
        iam_token = os.environ.get("IAM_TOKEN", "")
        assert len(iam_token) > 0, "Don't forget about export IAM_TOKEN="
        logging.info("found IAM token with length %d", len(iam_token))
        SAVED_IAM_TOKEN = iam_token
    return SAVED_IAM_TOKEN


def build_headers():
    iam_token = get_iam_token()
    headers = {
        "Authorization": f"Bearer {iam_token}"
    }
    return headers


def asr_async(args):
    audio_name = args.audio_name
    assert audio_name.endswith(".mp3")

    audio_url = f"https://storage.yandexcloud.net/kazakhverb/{audio_name}"
    logging.info("audio url %s", audio_url)

    url = f"https://{ENDPOINT}"

    logging.info("url %s", url)
    body_json = build_body_json(audio_url)
    logging.info("body json: %s", body_json)

    headers = build_headers()

    response = requests.post(
        url,
        json=body_json,
        headers=headers
    )
    response.raise_for_status()
    print(response.json())


def make_operation_status_filepath(operation_id):
    return f"operation_status_{operation_id}.json"


def check_status(args):
    operation_id = args.operation_id
    assert len(operation_id) > 0

    url = f"https://{STATUS_ENDPOINT}/operations/{operation_id}"
    headers = build_headers()

    response = requests.get(url, headers=headers)
    response.raise_for_status()

    filepath = make_operation_status_filepath(operation_id)
    with open(filepath, "wt") as output:
        output.write(response.text)
    logging.info("operation status is stored to %s", filepath)
    parsed = json.loads(response.text)
    if parsed["done"]:
        logging.info("%s DONE", operation_id)
    else:
        logging.info("%s IN PROGRESS", operation_id)


def parse_timestamp(s):
    assert s[-1] == "s"
    dotpos = s.find(".")
    if dotpos == -1:
        return int(s[:-1]) * 1000
    assert s[-5] == "."
    return int(s[:-5]) * 1000 + int(s[-4:-1])


def print_timestamp(t):
    d = t // 1000
    r = t - d * 1000
    return f"{d}.{r:03}"


def load_operation_result_chunks(operation_id):
    assert len(operation_id) > 0

    with open(make_operation_status_filepath(operation_id)) as inputfile:
        jsondoc = json.load(inputfile)
    if not jsondoc["done"]:
        logging.info("operation is not done: %s", str(jsondoc["done"]))
        return None
    return jsondoc["response"]["chunks"]


def extract_transcription(args):
    chunks = load_operation_result_chunks(args.operation_id)
    if chunks is None:
        return

    output_filepath = f"transcription_{args.operation_id}.jsonl"
    min_dur = 1000000000
    max_dur = 0
    with open(output_filepath, "wt") as output:
        prev_end = -1000000000
        for chunk in chunks:
            if chunk["channelTag"] != "1":
                continue
            startTime = 1000000000
            endTime = 0
            alt0 = chunk["alternatives"][0]
            converted_words = []
            for word in alt0["words"]:
                st = parse_timestamp(word["startTime"])
                et = parse_timestamp(word["endTime"])
                converted_words.append({
                    "word": word["word"],
                    "startTime": st,
                    "endTime": et,
                })
                startTime = min(startTime, st)
                endTime = max(endTime, et)
            if prev_end > startTime + 10:
                logging.error("overlap: prev end %d, cur start %d", prev_end, startTime)
            prev_end = endTime
            dur = endTime - startTime
            if min_dur > dur:
                min_dur = dur
                logging.info("Update min_dur: %s, %s", print_timestamp(min_dur), alt0["text"])
            if max_dur < dur:
                max_dur = dur
                logging.info("Update max_dur: %s, %s", print_timestamp(max_dur), alt0["text"])
            jline = {
                "text": alt0["text"],
                "startTime": startTime,
                "endTime": endTime,
                "words": converted_words,
            }
            output_line = json.dumps(jline, ensure_ascii=False)
            output.write(f"{output_line}\n")
    logging.info("Durations are from %s to %s", print_timestamp(min_dur), print_timestamp(max_dur))


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute(f"""
CREATE TABLE IF NOT EXISTS {TABLE_NAME} (
    video_id TEXT NOT NULL,
    start_ms INTEGER NOT NULL,
    end_ms INTEGER NOT NULL,
    content TEXT NOT NULL,
    words TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (video_id, start_ms)
);
    """.strip())

    logging.info("Database connection with %s established", db_path)
    return conn


def insert_records(db_conn, records):
    query = f"INSERT INTO {TABLE_NAME} (video_id, start_ms, end_ms, content, words) VALUES (?, ?, ?, ?, ?);"
    cursor = db_conn.cursor()
    cursor.executemany(query, records)
    db_conn.commit()
    logging.info("Inserted %d more subtitles into %s", len(records), TABLE_NAME)
    cursor.close()


def insert_subtitles(args):
    db_conn = init_db_conn(DB_PATH)

    inserted = 0
    accumulated = []

    chunks = load_operation_result_chunks(args.operation_id)
    if chunks is None:
        return

    prev_end = -1000000000
    for chunk in chunks:
        if chunk["channelTag"] != "1":
            continue
        startTime = 1000000000
        endTime = 0
        alt0 = chunk["alternatives"][0]
        converted_words = []
        for word in alt0["words"]:
            st = parse_timestamp(word["startTime"])
            et = parse_timestamp(word["endTime"])
            converted_words.append({
                "word": word["word"],
                "startTime": st,
                "endTime": et,
            })
            startTime = min(startTime, st)
            endTime = max(endTime, et)
        if prev_end > startTime + 10:
            logging.error("overlap: prev end %d, cur start %d", prev_end, startTime)
        prev_end = endTime
        accumulated.append((
            args.video_id,
            startTime,
            endTime,
            alt0["text"],
            json.dumps(
                {"words": converted_words},
                ensure_ascii=False
            )
        ))
        if len(accumulated) > 50:
            insert_records(db_conn, accumulated)
            inserted += len(accumulated)
            accumulated = []
    if len(accumulated) > 0:
        insert_records(db_conn, accumulated)
        inserted += len(accumulated)
        accumulated = []
    logging.info("Inserted %d subtitles for video %s", inserted, args.video_id)


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()

    subparsers = parser.add_subparsers()

    asr_async_parser = subparsers.add_parser("asr_async")
    asr_async_parser.add_argument("--audio-name", type=str, required=True)
    asr_async_parser.set_defaults(func=asr_async)

    check_status_parser = subparsers.add_parser("check_status")
    check_status_parser.add_argument("--operation-id", type=str, required=True)
    check_status_parser.set_defaults(func=check_status)

    extract_transcription_parser = subparsers.add_parser("extract_transcription")
    extract_transcription_parser.add_argument("--operation-id", type=str, required=True)
    extract_transcription_parser.set_defaults(func=extract_transcription)

    insert_subtitles_parser = subparsers.add_parser("insert_subtitles")
    insert_subtitles_parser.add_argument("--operation-id", type=str, required=True)
    insert_subtitles_parser.add_argument("--video-id", type=str, required=True)
    insert_subtitles_parser.set_defaults(func=insert_subtitles)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())
