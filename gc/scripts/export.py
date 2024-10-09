import argparse
import json
import logging
import sqlite3
import sys

DATABASE_PATH = "gc.db"


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


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    export_translations_parser = subparsers.add_parser("export-translations")
    export_translations_parser.add_argument("--db-path", default=DATABASE_PATH)
    export_translations_parser.add_argument("--jsonl", required=True)
    export_translations_parser.set_defaults(func=export_translations)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())