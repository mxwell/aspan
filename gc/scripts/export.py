import argparse
from datetime import datetime
import json
import logging
import os
import sqlite3
import sys

import boto3

DATABASE_PATH = "gc.db"
BUCKET_NAME="kazakhverbdict"
BUCKET_URL = f"https://storage.yandexcloud.net/{BUCKET_NAME}/"


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    logging.info("Database connection with %s established", db_path)
    return conn


def load_translation_pairs(db_conn, src_lang, dst_lang, offset, count):
    cursor = db_conn.cursor()
    cursor.execute("""
        SELECT
            w1.lang AS source_lang,
            w1.word_id AS source_word_id,
            w1.word AS source_word,
            w1.pos AS source_pos,
            w1.exc_verb AS source_exc_verb,
            w1.comment AS source_comment,
            w2.lang AS translation_lang,
            w2.word_id AS translation_word_id,
            w2.word AS translation_word,
            w2.pos AS translation_pos,
            w2.comment AS translation_comment
        FROM
            words w1
        JOIN
            translations t ON w1.word_id = t.word_id
        JOIN
            words w2 ON t.translated_word_id = w2.word_id
        WHERE
            w1.lang = ? AND
            w2.lang = ?
        ORDER BY w1.word, w1.pos, w1.exc_verb, w1.word_id, t.translation_id
        LIMIT ?, ?;
    """, (src_lang, dst_lang, offset, count))

    results = cursor.fetchall()

    translation_pairs = [
        {
            "source_lang": row["source_lang"],
            "source_word_id": row["source_word_id"],
            "source_word": row["source_word"],
            "source_pos": row["source_pos"],
            "source_exc_verb": row["source_exc_verb"],
            "source_comment": row["source_comment"],
            "translation_lang": row["translation_lang"],
            "translation_word_id": row["translation_word_id"],
            "translation_word": row["translation_word"],
            "translation_pos": row["translation_pos"],
            "translation_comment": row["translation_comment"],
        }
        for row in results
    ]

    return translation_pairs


def export_direction(db_conn, src_lang, dst_lang, jsonl_file):
    PAGE = 100
    exported = 0
    while True:
        translations = load_translation_pairs(db_conn, src_lang, dst_lang, exported, PAGE)
        if len(translations) == 0:
            break
        for translation in translations:
            jsonl_file.write(f"{json.dumps(translation)}\n")
        exported += len(translations)
        logging.info("exported %d translations for direction %s -> %s", exported, src_lang, dst_lang)
        if len(translations) < PAGE:
            break


def export_translations(args):
    db_conn = init_db_conn(args.db_path)

    with open(args.jsonl, "wt") as jsonl_file:
        export_direction(db_conn, "kk", "ru", jsonl_file)
        export_direction(db_conn, "kk", "en", jsonl_file)


def insert_download(db_conn, datestamp, url):
    download_id = int(datestamp)

    db_conn.execute("""
    CREATE TABLE IF NOT EXISTS downloads (
        id INTEGER PRIMARY KEY,
        url TEXT NOT NULL
    );""")

    insert_query = """INSERT INTO downloads (id, url) VALUES (?, ?)"""

    cursor = db_conn.cursor()
    cursor.execute(insert_query, (download_id, url))
    db_conn.commit()
    logging.info("Inserted a new download %d into DB", download_id)


def upload_export(args):
    assert os.path.exists(args.jsonl), f"path {args.jsonl} doesn't exist"

    db_conn = init_db_conn(args.db_path)

    today = datetime.now()
    datestamp = today.strftime("%Y%m%d")
    upload_name = f"kvd_translations_{datestamp}.jsonl"
    upload_url = f"{BUCKET_URL}{upload_name}"

    boto_session = boto3.session.Session()
    s3 = boto_session.client(
        service_name="s3",
        endpoint_url="https://storage.yandexcloud.net"
    )

    s3.upload_file(args.jsonl, BUCKET_NAME, upload_name)
    logging.info("Uploaded export %s to S3", upload_name)
    logging.info("URL: %s", upload_url)

    insert_download(db_conn, datestamp, upload_url)


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    export_translations_parser = subparsers.add_parser("export-translations")
    export_translations_parser.add_argument("--db-path", default=DATABASE_PATH)
    export_translations_parser.add_argument("--jsonl", required=True)
    export_translations_parser.set_defaults(func=export_translations)

    upload_export_parser = subparsers.add_parser("upload-export")
    upload_export_parser.add_argument("--jsonl", required=True)
    upload_export_parser.add_argument("--db-path", default=DATABASE_PATH)
    upload_export_parser.set_defaults(func=upload_export)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())