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
            w2.word AS translation_word,
            w2.pos AS translation_pos
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
                "translation_word": row["translation_word"],
                "translation_pos": row["translation_pos"],
            }
            for row in results
        ]

        return translations

    def do_get_inversed_translations(self, src_lang, dst_lang, word):
        query = """
        SELECT
            w1.word AS source_word,
            w1.pos AS source_pos,
            w2.word AS translation_word,
            w2.pos AS translation_pos
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
                "translation_word": row["translation_word"],
                "translation_pos": row["translation_pos"],
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


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("""
CREATE TABLE IF NOT EXISTS words (
    word_id INTEGER PRIMARY KEY,
    word TEXT NOT NULL,
    pos TEXT NOT NULL,
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


@app.route("/api/v1/test", methods=["GET"])
def get_test():
    return jsonify({"message": "You've reached GC!"}), 200


@app.route("/api/v1/check_user", methods=["POST"])
def post_check_user():
    global gc_instance

    request_data = request.json
    message, code = gc_instance.check_user(request_data)
    return jsonify({"message": message}), code


@app.route("/api/v1/create_user", methods=["POST"])
def post_create_user():
    global gc_instance

    request_data = request.json
    message, token, code = gc_instance.create_user(request_data)
    return jsonify({"message": message, "token": token}), code


@app.route("/api/v1/get_token", methods=["POST"])
def post_get_token():
    global gc_instance

    request_data = request.json
    message, token, code = gc_instance.get_token(request_data)
    return jsonify({"message": message, "token": token}), code


@app.route("/api/v1/get_translation", methods=["GET"])
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


def main():
    return 0


if __name__ == '__main__':
    sys.exit(main())
else:
    init_gc_app()
