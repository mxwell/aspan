import argparse
import logging
from logging.config import dictConfig
import os
from os.path import join as pj
import requests
import sqlite3
import sys
import threading
import uuid

from flask import Flask, jsonify, redirect, request, make_response, send_file

from lib.auth import Auth
from lib.pos import parse_pos
from lib.word_info import WordInfo


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
DATABASE_PATH = "gc.db"
app = Flask("gc_app")
gc_instance = None


def validate_lang(lang):
    return lang == "en" or lang == "kk" or lang == "ru"


def read_words(fetched_results):
    return [
        WordInfo(
            row["word_id"],
            row["word"],
            row["pos"],
            row["exc_verb"] > 0,
            row["lang"],
            row["comment"],
            int(row["created_at_unix_epoch"]),
        )
        for row in fetched_results
    ]


class InsertionResult(object):

    # One of args must be None
    def __init__(self, translation_id, error_message):
        self.translation_id = translation_id
        self.error_message = error_message


class Gc(object):

    def __init__(self, db_conn, auth):
        self.db_lock = threading.Lock()
        self.db_conn = db_conn
        self.auth = auth

    def check_user(self, request_data):
        return self.auth.check_user(request_data, self.db_lock, self.db_conn)

    def create_user(self, request_data):
        return self.auth.create_user(request_data, self.db_lock, self.db_conn)

    def get_token(self, request_data):
        return self.auth.get_token(request_data, self.db_lock, self.db_conn)

    def do_get_translations(self, src_lang, dst_lang, word):
        query = """
        SELECT
            w1.word AS source_word,
            w1.pos AS source_pos,
            w1.exc_verb AS source_exc_verb,
            w1.comment AS source_comment,
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
            w1.word = ?
            AND w1.lang = ?
            AND w2.lang = ?
        LIMIT 100;
        """

        cursor = self.db_conn.cursor()
        cursor.execute(query, (word, src_lang, dst_lang))

        results = cursor.fetchall()

        translations = [
            {
                "word": row["source_word"],
                "pos": row["source_pos"],
                "exc_verb": row["source_exc_verb"],
                "comment": row["source_comment"],
                "translation_word": row["translation_word"],
                "translation_pos": row["translation_pos"],
                "translation_comment": row["translation_comment"]
            }
            for row in results
        ]

        return translations

    def do_get_inversed_translations(self, src_lang, dst_lang, word):
        query = """
        SELECT
            w1.word AS source_word,
            w1.pos AS source_pos,
            w1.exc_verb AS source_exc_verb,
            w1.comment AS source_comment,
            w2.word AS translation_word,
            w2.pos AS translation_pos,
            w2.comment AS translation_comment
        FROM
            words w1
        JOIN
            translations t ON w1.word_id = t.translated_word_id
        JOIN
            words w2 ON t.word_id = w2.word_id
        WHERE
            w1.word = ?
            AND w1.lang = ?
            AND w2.lang = ?
        LIMIT 100;
        """

        cursor = self.db_conn.cursor()
        cursor.execute(query, (word, src_lang, dst_lang))

        results = cursor.fetchall()

        translations = [
            {
                "word": row["source_word"],
                "pos": row["source_pos"],
                "exc_verb": row["source_exc_verb"],
                "comment": row["source_comment"],
                "translation_word": row["translation_word"],
                "translation_pos": row["translation_pos"],
                "translation_comment": row["translation_comment"]
            }
            for row in results
        ]

        return translations

    def get_translation(self, src_lang, dst_lang, word):
        with self.db_lock:
            if src_lang == "kk":
                return self.do_get_translations(src_lang, dst_lang, word)
            else:
                return self.do_get_inversed_translations(src_lang, dst_lang, word)

    def do_get_words(self, word, lang):
        query = """
        SELECT
            word_id,
            word,
            pos,
            exc_verb,
            lang,
            comment,
            strftime('%s', created_at) as created_at_unix_epoch
        FROM words
        WHERE
            word = ?
            AND lang = ?
        ORDER BY word_id
        LIMIT 100;
        """

        cursor = self.db_conn.cursor()
        cursor.execute(query, (word, lang))

        fetched_results = cursor.fetchall()
        return read_words(fetched_results)

    def get_words(self, word, lang):
        with self.db_lock:
            return self.do_get_words(word, lang)

    def count_words(self, word, pos, exc_verb, lang, comment):
        query = """
        SELECT COUNT(*)
        FROM words
        WHERE word = ?
        AND pos = ?
        AND exc_verb = ?
        AND lang = ?
        AND comment = ?
        ;
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (word, pos, exc_verb, lang, comment))
        count = cursor.fetchone()[0]
        cursor.close()
        return count

    # Returns ID of an inserted word or None
    def do_add_word(self, word, pos, exc_verb, lang, comment, user_id):
        existing = self.count_words(word, pos, exc_verb, comment, lang)
        if existing > 0:
            logging.error("do_add_word: %d existing words", existing)
            return None

        query = """
        INSERT INTO words (word, pos, exc_verb, lang, comment, user_id) VALUES (?, ?, ?, ?, ?, ?);
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (word, pos, exc_verb, lang, comment, user_id))
        self.db_conn.commit()
        return cursor.lastrowid

    # Returns ID of an inserted word or None
    def add_word(self, word, pos, exc_verb, lang, comment, user_id):
        with self.db_lock:
            return self.do_add_word(word, pos, exc_verb, lang, comment, user_id)

    # Returns WordInfo or None
    def do_get_word_by_id(self, word_id):
        query = """
        SELECT
            word_id,
            word,
            pos,
            exc_verb,
            lang,
            comment,
            strftime('%s', created_at) as created_at_unix_epoch
        FROM words
        WHERE
            word_id = ?
        LIMIT 10;
        """

        cursor = self.db_conn.cursor()
        cursor.execute(query, (word_id,))

        fetched_results = cursor.fetchall()
        words = read_words(fetched_results)
        if len(words) == 1:
            return words[0]
        logging.error("do_get_word_by_id: unexpected number of words for ID %d: %d", word_id, len(words))
        return None

    def count_translations_with_word_ids(self, src_id, dst_id):
        query = """
        SELECT COUNT(*)
        FROM translations
        WHERE word_id = ?
        AND translated_word_id = ?;
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (src_id, dst_id))
        count = cursor.fetchone()[0]
        cursor.close()
        return count

    # Returns InsertionResult
    def do_add_translation(self, src_id, dst_id, reference, user_id):
        src_word = self.do_get_word_by_id(src_id)
        if src_word is None or src_word.lang != "kk":
            logging.error("do_add_translation: invalid src word ID %d", src_id)
            return InsertionResult(None, "invalid_src")

        dst_word = self.do_get_word_by_id(dst_id)
        if dst_word is None or dst_word.lang == "kk":
            logging.error("do_add_translation: invalid dst word ID %d", dst_id)
            return InsertionResult(None, "invalid_dst")

        existing = self.count_translations_with_word_ids(src_id, dst_id)
        if existing > 0:
            logging.error("do_add_translation: %d existing translations", existing)
            return InsertionResult(None, "duplicate")

        query = """
        INSERT INTO translations (word_id, translated_word_id, reference, user_id) VALUES (?, ?, ?, ?);
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (src_id, dst_id, reference, user_id))
        self.db_conn.commit()
        return InsertionResult(cursor.lastrowid, None)

    # Returns InsertionResult
    def add_translation(self, src_id, dst_id, reference, user_id):
        with self.db_lock:
            return self.do_add_translation(src_id, dst_id, reference, user_id)

    # Returns user ID or None
    def get_user_id_from_header(self, headers):
        auth_header = headers.get("Authorization")
        if not auth_header:
            logging.error("get_user_id_from_header: no header")
            return None
        if not auth_header.startswith("Bearer "):
            logging.error("get_user_id_from_header: invalid header format: %s", auth_header)
            return None
        token = auth_header[7:]
        return self.auth.extract_user_id_from_token(token)

    def do_extract_feed(self):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                u.name AS name,
                w1.word AS src_word,
                w1.lang AS src_lang,
                w2.word AS dst_word,
                w2.lang AS dst_lang,
                strftime('%s', t.created_at) AS created_at
            FROM
                translations t
            JOIN
                users u ON t.user_id = u.user_id
            JOIN
                words w1 ON t.word_id = w1.word_id
            JOIN
                words w2 ON t.translated_word_id = w2.word_id
            WHERE t.created_at >= datetime('now', '-2 days')
            ORDER BY t.created_at DESC
            LIMIT 100;
        """)

        results = cursor.fetchall()

        translations = [
            {
                "name": row["name"],
                "src_word": row["src_word"],
                "src_lang": row["src_lang"],
                "dst_word": row["dst_word"],
                "dst_lang": row["dst_lang"],
                "created_at": int(row["created_at"]),
            }
            for row in results
        ]

        return translations

    def get_feed(self):
        with self.db_lock:
            return self.do_extract_feed()


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("""
CREATE TABLE IF NOT EXISTS words (
    word_id INTEGER PRIMARY KEY,
    word TEXT NOT NULL,
    pos TEXT NOT NULL,
    exc_verb INT NOT NULL DEFAULT 0,
    comment TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    lang TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_word ON words(word);
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS translations (
    translation_id INTEGER PRIMARY KEY,
    word_id INTEGER NOT NULL,
    translated_word_id INTEGER NOT NULL,
    reference TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_translation ON translations(word_id);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_translation_inv ON translations(translated_word_id);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_translation_timestamps ON translations(created_at);
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY,
    email TEXT NOT NULL,
    email_verified INTEGER NOT NULL,
    sub TEXT NOT NULL,
    name TEXT NOT NULL,
    locale TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())
    conn.execute("""
CREATE INDEX IF NOT EXISTS idx_users ON users(email);
    """.strip())

    logging.info("Database connection with %s established", db_path)
    return conn


def init_gc_app():
    global gc_instance
    db_conn = init_db_conn(DATABASE_PATH)
    auth = Auth()
    gc_instance = Gc(db_conn, auth)
    logging.info("GC app initialized")


def valid_lang(lang):
    return lang == "en" or lang == "kk" or lang == "ru"


@app.route("/gcapi/v1/test", methods=["GET"])
def get_test():
    return jsonify({"message": "You've reached GC!"}), 200


@app.route("/gcapi/v1/check_user", methods=["POST"])
def post_check_user():
    global gc_instance

    request_data = request.json
    message, code = gc_instance.check_user(request_data)
    return jsonify({"message": message}), code


@app.route("/gcapi/v1/create_user", methods=["POST"])
def post_create_user():
    global gc_instance

    request_data = request.json
    message, token, code = gc_instance.create_user(request_data)
    return jsonify({"message": message, "token": token}), code


@app.route("/gcapi/v1/get_token", methods=["POST"])
def post_get_token():
    global gc_instance

    request_data = request.json
    message, token, code = gc_instance.get_token(request_data)
    return jsonify({"message": message, "token": token}), code


@app.route("/gcapi/v1/get_translation", methods=["GET"])
def get_translation():
    global gc_instance

    src_lang = request.args.get("src")
    dst_lang = request.args.get("dst")
    word = request.args.get("w")

    logging.info("Request /get_translation %s->%s: %s", src_lang, dst_lang, word)

    if not valid_lang(src_lang):
        logging.error("Invalid src")
        return jsonify({"message": "Invalid request"}), 400
    if not valid_lang(dst_lang):
        logging.error("Invalid dst")
        return jsonify({"message": "Invalid request"}), 400
    if word is None or not (0 < len(word) < 64):
        logging.error("Invalid word")
        return jsonify({"message": "Invalid request"}), 400

    translations = gc_instance.get_translation(src_lang, dst_lang, word)
    return jsonify({"translations": translations}), 200


@app.route("/gcapi/v1/get_words", methods=["GET"])
def get_words():
    global gc_instance

    word = request.args.get("w")
    lang = request.args.get("lang")

    if not word:
        logging.error("Invalid word")
        return jsonify({"message": "Invalid word"}), 400
    if not validate_lang(lang):
        logging.error("Invalid language")
        return jsonify({"message": "Invalid language"}), 400

    words = gc_instance.get_words(word, lang)
    return jsonify({"words": words}), 200


@app.route("/gcapi/v1/add_word", methods=["POST"])
def post_add_word():
    global gc_instance

    user_id = gc_instance.get_user_id_from_header(request.headers)
    if not user_id:
        return jsonify({"message": "Unauthorized"}), 401

    request_data = request.json
    word = request_data.get("w")
    pos = request_data.get("pos")
    exc_verb = request_data.get("ev", False) == True
    lang = request_data.get("lang")
    comment = request_data.get("com")

    if not word:
        logging.error("Invalid word")
        return jsonify({"message": "Invalid word"}), 400
    posTag = parse_pos(pos)
    if posTag is None:
        logging.error("Invalid pos: %s", pos)
        return jsonify({"message": "Invalid pos"}), 400
    if not validate_lang(lang):
        logging.error("Invalid language")
        return jsonify({"message": "Invalid language"}), 400
    if comment is None or len(comment) > 256:
        logging.error("Invalid comment")
        return jsonify({"message": "Invalid comment"}), 400

    word_id = gc_instance.add_word(word, pos, exc_verb, lang, comment, user_id)
    if word_id is None:
        logging.error("No word_id after insertion")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "word_id": word_id}), 201


@app.route("/gcapi/v1/add_translation", methods=["POST"])
def post_add_translation():
    global gc_instance

    user_id = gc_instance.get_user_id_from_header(request.headers)
    if not user_id:
        return jsonify({"message": "Unauthorized"}), 401

    request_data = request.json
    src_id = request_data.get("src")
    dst_id = request_data.get("dst")
    reference = request_data.get("ref")

    if not isinstance(src_id, int):
        logging.error("Invalid src: %s", str(src_id))
        return jsonify({"message": "Invalid src"}), 400
    if not isinstance(dst_id, int):
        logging.error("Invalid dst: %s", str(dst_id))
        return jsonify({"message": "Invalid dst"}), 400
    if reference is None or len(reference) > 256:
        logging.error("Invalid reference: %s", str(reference))
        return jsonify({"message": "Invalid reference"}), 400

    insertion_result = gc_instance.add_translation(
        int(src_id),
        int(dst_id),
        reference,
        user_id,
    )
    if insertion_result is None:
        logging.error("No insertion_result after insertion")
        return jsonify({"message": "Internal error"}), 500
    translation_id = insertion_result.translation_id
    if translation_id is None:
        logging.error("No translation_id after insertion: %s", insertion_result.error_message)
        return jsonify({"message": insertion_result.error_message}), 500
    return jsonify({"message": "ok", "translation_id": translation_id}), 201


@app.route("/gcapi/v1/get_feed", methods=["GET"])
def get_feed():
    global gc_instance

    feed = gc_instance.get_feed()
    if feed is None:
        logging.error("null feed")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "feed": feed}), 200


def main():
    return 0


if __name__ == '__main__':
    sys.exit(main())
else:
    init_gc_app()
