import argparse
import logging
from logging.config import dictConfig
import os
from os.path import join as pj
import random
import requests
import sqlite3
import sys
import threading
import time
import uuid

from flask import Flask, jsonify, redirect, request, make_response, send_file

from lib.auth import Auth
from lib.feed import FeedItem, VoteInfo
from lib.pos import parse_pos
from lib.review import ReviewStatus, ReviewVote
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
CACHE_TTL_SECS = 300
REVIEW_PAGE_SIZE = 20
APPROVE_THRESHOLD = 2
DISAPPROVE_THRESHOLD = 2
app = Flask("gc_app")
gc_instance = None


def validate_lang(lang):
    return lang == "en" or lang == "kk" or lang == "ru"


def read_words(fetched_results):
    grouped = dict()
    for row in fetched_results:
        word_id = row["word_id"]
        if word_id in grouped:
            grouped[word_id].append(row)
        else:
            grouped[word_id] = [row]

    result = []
    for word_id, group in grouped.items():
        translated_word_ids = set()
        review_word_ids = set()
        assert len(group) > 0
        for row in group:
            has_translation = "translated_word_id" in row.keys()
            if has_translation and row["translated_word_id"]:
                translated_word_ids.add(row["translated_word_id"])
            has_review = "review_word_id" in row.keys()
            if has_review and row["review_word_id"]:
                review_status = row["review_status"]
                if review_status == ReviewStatus.NEW.name:
                    review_word_ids.add(row["review_word_id"])
        row = group[0]
        result.append(
            WordInfo(
                row["word_id"],
                row["word"],
                row["pos"],
                row["exc_verb"] > 0,
                row["lang"],
                row["comment"],
                int(row["created_at_unix_epoch"]),
                sorted(translated_word_ids),
                sorted(review_word_ids),
            )
        )
    return result


def parse_vote(vote):
    if vote:
        try:
            return ReviewVote[vote].value
        except KeyError as e:
            logging.error("parse_vote: unknown vote [%s]", vote)
    return None


def read_feed_items(fetched_results):
    result = []
    prev_tr_id = None
    for row in fetched_results:
        translation_id = row["tr_id"]
        assert translation_id

        if translation_id != prev_tr_id:
            result.append(
                FeedItem(
                    row["name"],
                    row["src_word"],
                    row["src_lang"],
                    row["dst_word"],
                    row["dst_lang"],
                    translation_id,
                    [], # votes
                    int(row["created_at"]),
                )
            )
            prev_tr_id = translation_id

        vote = parse_vote(row["vote"])
        voter = row["voter"]
        if vote and voter:
            result[-1].votes.append(VoteInfo(vote, voter))

    return result


class InsertionResult(object):

    # One of args must be None
    def __init__(self, inserted_id, error_message):
        self.inserted_id = inserted_id
        self.error_message = error_message

class AddReviewVoteResult(object):

    def __init__(self, inserted, approves, disapproves, own_approves, own_disapproves, gone, error_message):
        self.inserted = inserted
        self.approves = approves
        self.disapproves = disapproves
        self.own_approves = own_approves
        self.own_disapproves = own_disapproves
        self.gone = gone
        self.error_message = error_message

    def make_response(self, message):
        return {
            "message": message,
            "approves": self.approves,
            "disapproves": self.disapproves,
            "own_approves": self.own_approves,
            "gone": self.gone,
            "own_disapproves": self.own_disapproves,
        }


class GcCache(object):

    def __init__(self):
        self.stats = None
        self.expiration = None

    def update_stats(self, stats):
        self.stats = stats
        self.expiration = time.time() + CACHE_TTL_SECS

    def get_stats(self):
        if self.stats:
            if self.expiration < time.time():
                self.stats = None
            else:
                return self.stats
        return None


class UntranslatedCache(object):

    def __init__(self):
        self.untranslated = None
        self.expiration = None
        self.dropped_word_ids = set()

    def drop_item(self, word_id):
        self.dropped_word_ids.add(word_id)
        logging.info("Dropped word_id %d from untranslated cache", word_id)

    def do_pick_random(self, count):
        for it in range(3):
            offset = random.randint(0, len(self.untranslated) - count)
            result = []
            for index in range(offset, offset + count):
                word_id, word = self.untranslated[index]
                if word_id in self.dropped_word_ids:
                    continue
                result.append(word)
            if len(result) >= count:
                break
        return result

    def pick_random(self, count):
        if self.untranslated:
            if self.expiration < time.time():
                self.untranslated = None
                return None
            if len(self.untranslated) < count:
                logging.error("pick_random: not enough items in untranslated cache")
                return None
            return self.do_pick_random(count)
        return None

    def reset(self, untranslated):
        self.untranslated = untranslated
        self.expiration = time.time() + CACHE_TTL_SECS
        self.dropped_word_ids = set()


class Gc(object):

    def __init__(self, db_conn, auth):
        self.db_lock = threading.Lock()
        self.db_conn = db_conn
        self.auth = auth
        self.cache = GcCache()
        self.untranslated_cache = UntranslatedCache()

    def check_user(self, request_data):
        return self.auth.check_user(request_data, self.db_lock, self.db_conn)

    def create_user(self, request_data):
        return self.auth.create_user(request_data, self.db_lock, self.db_conn)

    def get_token(self, request_data):
        return self.auth.get_token(request_data, self.db_lock, self.db_conn)

    def do_get_translations(self, src_lang, dst_lang, both_dirs, word):
        cursor = self.db_conn.cursor()
        if both_dirs:
            cursor.execute("""
                SELECT
                    t.translation_id AS translation_id,
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
                    (w1.word = ? OR w2.word = ?)
                    AND w1.lang = ?
                    AND w2.lang = ?
                LIMIT 100;
            """, (word, word, src_lang, dst_lang))
        else:
            cursor.execute("""
                SELECT
                    t.translation_id AS translation_id,
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
            """, (word, src_lang, dst_lang))

        results = cursor.fetchall()

        translations = [
            {
                "translation_id": row["translation_id"],
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

    def do_get_inversed_translations(self, src_lang, dst_lang, both_dirs, word):
        cursor = self.db_conn.cursor()
        if both_dirs:
            cursor.execute("""
                SELECT
                    t.translation_id AS translation_id,
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
                    (w1.word = ? OR w2.word = ?)
                    AND w1.lang = ?
                    AND w2.lang = ?
                LIMIT 100;
            """, (word, word, src_lang, dst_lang))
        else:
            cursor.execute("""
                SELECT
                    t.translation_id AS translation_id,
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
            """, (word, src_lang, dst_lang))

        results = cursor.fetchall()

        translations = [
            {
                "translation_id": row["translation_id"],
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

    def get_translations(self, src_lang, dst_lang, both_dirs, word):
        with self.db_lock:
            if src_lang == "kk":
                return self.do_get_translations(src_lang, dst_lang, both_dirs, word)
            else:
                return self.do_get_inversed_translations(src_lang, dst_lang, both_dirs, word)

    def do_get_translation_info(self, translation_id):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                t.translation_id AS translation_id,
                t.reference AS reference,
                t.user_id AS user_id,
                u.name AS user_name,
                w1.word AS source_word,
                w1.pos AS source_pos,
                w1.exc_verb AS source_exc_verb,
                w1.comment AS source_comment,
                w2.word AS translation_word,
                w2.pos AS translation_pos,
                w2.comment AS translation_comment
            FROM
                translations t
            JOIN
                words w1 ON t.word_id = w1.word_id
            JOIN
                words w2 ON t.translated_word_id = w2.word_id
            JOIN
                users u ON t.user_id = u.user_id
            WHERE
                t.translation_id = ?
            ;
        """, (translation_id,))

        results = cursor.fetchall()

        translations = [
            {
                "translation_id": row["translation_id"],
                "reference": row["reference"],
                "user_id": row["user_id"],
                "user_name": row["user_name"],
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

    def get_translation_info(self, translation_id):
        with self.db_lock:
            return self.do_get_translation_info(translation_id)

    def do_get_words(self, word, lang, with_translations):
        cursor = self.db_conn.cursor()
        if with_translations:
            cursor.execute("""
                SELECT
                    w1.word_id AS word_id,
                    w1.word AS word,
                    w1.pos AS pos,
                    w1.exc_verb AS exc_verb,
                    w1.lang AS lang,
                    w1.comment AS comment,
                    strftime('%s', w1.created_at) as created_at_unix_epoch,
                    t.translated_word_id AS translated_word_id,
                    r.translated_word_id AS review_word_id,
                    r.status AS review_status
                FROM
                    words w1
                LEFT JOIN
                    translations t ON w1.word_id = t.word_id
                LEFT JOIN
                    reviews r ON w1.word_id = r.word_id
                WHERE
                    w1.word = ?
                    AND w1.lang = ?
                LIMIT 100;
            """, (word, lang))
        else:
            cursor.execute("""
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
            """, (word, lang))

        fetched_results = cursor.fetchall()
        words = read_words(fetched_results)
        cursor.close()
        return words

    def get_words(self, word, lang, with_translations):
        with self.db_lock:
            return self.do_get_words(word, lang, with_translations)

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
        cursor.close()
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

        logging.info("Inserting translation %d -> %s by user %d", src_id, dst_id, user_id)
        query = """
        INSERT INTO translations (word_id, translated_word_id, reference, user_id) VALUES (?, ?, ?, ?);
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (src_id, dst_id, reference, user_id))
        self.db_conn.commit()
        return InsertionResult(cursor.lastrowid, None)

    # Returns InsertionResult
    def add_translation(self, src_id, dst_id, reference, user_id):
        self.untranslated_cache.drop_item(src_id)
        with self.db_lock:
            return self.do_add_translation(src_id, dst_id, reference, user_id)

    # Returns InsertionResult
    def do_add_review(self, src_id, dst_id, reference, user_id):
        src_word = self.do_get_word_by_id(src_id)
        if src_word is None or src_word.lang != "kk":
            logging.error("do_add_review: invalid src word ID %d", src_id)
            return InsertionResult(None, "invalid_src")

        dst_word = self.do_get_word_by_id(dst_id)
        if dst_word is None or dst_word.lang == "kk":
            logging.error("do_add_review: invalid dst word ID %d", dst_id)
            return InsertionResult(None, "invalid_dst")

        existing = self.count_translations_with_word_ids(src_id, dst_id)
        if existing > 0:
            logging.error("do_add_review: %d existing translations", existing)
            return InsertionResult(None, "duplicate")

        query = """
        INSERT INTO reviews (word_id, translated_word_id, reference, user_id, status) VALUES (?, ?, ?, ?, ?);
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (src_id, dst_id, reference, user_id, ReviewStatus.NEW.name))
        self.db_conn.commit()
        return InsertionResult(cursor.lastrowid, None)

    # Returns InsertionResult
    def add_review(self, src_id, dst_id, reference, user_id):
        self.untranslated_cache.drop_item(src_id)
        with self.db_lock:
            return self.do_add_review(src_id, dst_id, reference, user_id)

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

    def do_get_review_by_id(self, review_id):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT *
            FROM reviews
            WHERE review_id = ?;
        """, (review_id,))

        results = cursor.fetchall()

        reviews = [
            {
                "word_id": row["word_id"],
                "translated_word_id": row["translated_word_id"],
                "reference": row["reference"],
                "user_id": row["user_id"],
                "status": row["status"],
            }
            for row in results
        ]
        cursor.close()

        if len(reviews) != 1:
            logging.error("do_get_review_by_id: unexpected number of reviews for id %d: %d", review_id, len(reviews))
            return None

        return reviews[0]

    def do_get_reviews(self, user_id, approves_min, page):
        cursor = self.db_conn.cursor()
        cursor.execute(f"""
            SELECT
                r.review_id as review_id,
                u.user_id AS user_id,
                u.name AS name,
                w1.word AS src_word,
                w1.pos AS src_pos,
                w1.exc_verb AS src_exc_verb,
                w1.comment AS src_comment,
                w1.lang AS src_lang,
                w2.word AS dst_word,
                w2.pos AS dst_pos,
                w2.exc_verb AS dst_exc_verb,
                w2.comment AS dst_comment,
                w2.lang AS dst_lang,
                r.reference AS reference,
                r.status AS status,
                rv.approves AS approves,
                rv.disapproves AS disapproves,
                rv.own_approves AS own_approves,
                rv.own_disapproves AS own_disapproves,
                strftime('%s', r.created_at) AS created_at
            FROM
                reviews r
            JOIN
                users u ON r.user_id = u.user_id
            JOIN
                words w1 ON r.word_id = w1.word_id
            JOIN
                words w2 ON r.translated_word_id = w2.word_id
            LEFT JOIN (
                SELECT
                    review_id,
                    COUNT(CASE WHEN vote = "APPROVE" THEN 1 END) AS approves,
                    COUNT(CASE WHEN vote = "DISAPPROVE" THEN 1 END) AS disapproves,
                    COUNT(CASE WHEN user_id = ? AND vote = "APPROVE" THEN 1 END) AS own_approves,
                    COUNT(CASE WHEN user_id = ? AND vote = "DISAPPROVE" THEN 1 END) AS own_disapproves
                FROM review_votes
                GROUP BY review_id
            ) rv ON r.review_id = rv.review_id
            WHERE
                r.status == "NEW" AND
                COALESCE(rv.approves, 0) >= ?
            ORDER BY r.created_at DESC
            LIMIT {page * REVIEW_PAGE_SIZE},{REVIEW_PAGE_SIZE};
        """, (user_id, user_id, approves_min))

        results = cursor.fetchall()

        reviews = [
            {
                "review_id": row["review_id"],
                "user_id": row["user_id"],
                "name": row["name"],
                "src_word": row["src_word"],
                "src_pos": row["src_pos"],
                "src_exc_verb": row["src_exc_verb"],
                "src_comment": row["src_comment"],
                "src_lang": row["src_lang"],
                "dst_word": row["dst_word"],
                "dst_pos": row["dst_pos"],
                "dst_exc_verb": row["dst_exc_verb"],
                "dst_comment": row["dst_comment"],
                "dst_lang": row["dst_lang"],
                "reference": row["reference"],
                "status": row["status"],
                "approves": row["approves"] or 0,
                "disapproves": row["disapproves"] or 0,
                "own_approves": row["own_approves"] or 0,
                "own_disapproves": row["own_disapproves"] or 0,
                "created_at": int(row["created_at"]),
            }
            for row in results
        ]
        cursor.close()

        return reviews

    def get_reviews(self, user_id, approves_min, page):
        with self.db_lock:
            return self.do_get_reviews(user_id, approves_min, page)

    def do_get_reviews_by_dir(self, user_id, src_lang, dst_lang, page):
        cursor = self.db_conn.cursor()
        cursor.execute(f"""
            SELECT
                r.review_id as review_id,
                u.user_id AS user_id,
                u.name AS name,
                w1.word AS src_word,
                w1.pos AS src_pos,
                w1.exc_verb AS src_exc_verb,
                w1.comment AS src_comment,
                w1.lang AS src_lang,
                w2.word AS dst_word,
                w2.pos AS dst_pos,
                w2.exc_verb AS dst_exc_verb,
                w2.comment AS dst_comment,
                w2.lang AS dst_lang,
                r.reference AS reference,
                r.status AS status,
                rv.approves AS approves,
                rv.disapproves AS disapproves,
                rv.own_approves AS own_approves,
                rv.own_disapproves AS own_disapproves,
                strftime('%s', r.created_at) AS created_at
            FROM
                reviews r
            JOIN
                users u ON r.user_id = u.user_id
            JOIN
                words w1 ON r.word_id = w1.word_id
            JOIN
                words w2 ON r.translated_word_id = w2.word_id
            LEFT JOIN (
                SELECT
                    review_id,
                    COUNT(CASE WHEN vote = "APPROVE" THEN 1 END) AS approves,
                    COUNT(CASE WHEN vote = "DISAPPROVE" THEN 1 END) AS disapproves,
                    COUNT(CASE WHEN user_id = ? AND vote = "APPROVE" THEN 1 END) AS own_approves,
                    COUNT(CASE WHEN user_id = ? AND vote = "DISAPPROVE" THEN 1 END) AS own_disapproves
                FROM review_votes
                GROUP BY review_id
            ) rv ON r.review_id = rv.review_id
            WHERE r.status = "NEW"
                AND w1.lang = ?
                AND w2.lang = ?
            ORDER BY r.created_at DESC
            LIMIT {page * REVIEW_PAGE_SIZE},{REVIEW_PAGE_SIZE};
        """, (user_id, user_id, src_lang, dst_lang))

        results = cursor.fetchall()

        reviews = [
            {
                "review_id": row["review_id"],
                "user_id": row["user_id"],
                "name": row["name"],
                "src_word": row["src_word"],
                "src_pos": row["src_pos"],
                "src_exc_verb": row["src_exc_verb"],
                "src_comment": row["src_comment"],
                "src_lang": row["src_lang"],
                "dst_word": row["dst_word"],
                "dst_pos": row["dst_pos"],
                "dst_exc_verb": row["dst_exc_verb"],
                "dst_comment": row["dst_comment"],
                "dst_lang": row["dst_lang"],
                "reference": row["reference"],
                "status": row["status"],
                "approves": row["approves"] or 0,
                "disapproves": row["disapproves"] or 0,
                "own_approves": row["own_approves"] or 0,
                "own_disapproves": row["own_disapproves"] or 0,
                "created_at": int(row["created_at"]),
            }
            for row in results
        ]
        cursor.close()

        return reviews

    def get_reviews_by_dir(self, user_id, src_lang, dst_lang, page):
        with self.db_lock:
            return self.do_get_reviews_by_dir(user_id, src_lang, dst_lang, page)

    def count_review_votes_groupped(self, user_id, review_id):
        query = """
        SELECT
            COUNT(CASE WHEN vote = "APPROVE" THEN 1 END) AS approves,
            COUNT(CASE WHEN vote = "DISAPPROVE" THEN 1 END) AS disapproves,
            COUNT(CASE WHEN user_id = ? AND vote = "APPROVE" THEN 1 END) AS own_approves,
            COUNT(CASE WHEN user_id = ? AND vote = "DISAPPROVE" THEN 1 END) AS own_disapproves
        FROM review_votes
        WHERE review_id = ?;
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (user_id, user_id, review_id,))
        result = cursor.fetchone()
        approves = result["approves"]
        disapproves = result["disapproves"]
        own_approves = result["own_approves"]
        own_disapproves = result["own_disapproves"]
        logging.info("count_review_votes_groupped: review_id %d: %d vs %d, own %d vs %d", review_id, approves, disapproves, own_approves, own_disapproves)
        cursor.close()
        return (approves, disapproves, own_approves, own_disapproves)

    def do_move_votes_from_review_to_translation(self, review_id, translation_id):
        logging.info("Moving votes from review %d to translation %d", review_id, translation_id)
        cursor = self.db_conn.cursor()
        query = """
        INSERT INTO translation_votes (translation_id, user_id, vote)
        SELECT ?, user_id, vote
        FROM review_votes
        WHERE review_id = ?;
        """
        cursor.execute(query, (translation_id, review_id))
        self.db_conn.commit()

    # Returns InsertionResult
    def do_copy_review_to_translations(self, review_id):
        review = self.do_get_review_by_id(review_id)
        if review is None:
            logging.info("do_copy_review_to_translations: failed to get a unique review by id")
            return InsertionResult(None, "no review")
        if review["status"] != ReviewStatus.NEW.name:
            logging.info("do_copy_review_to_translations: unexpected review status %s", review["status"])
            return InsertionResult(None, "no review")

        insertion_result = self.do_add_translation(
            review["word_id"],
            review["translated_word_id"],
            review["reference"],
            review["user_id"],
        )
        if insertion_result.inserted_id is None:
            logging.info("do_copy_review_to_translations: failed to insert translation")
            return insertion_result

        self.do_move_votes_from_review_to_translation(review_id, insertion_result.inserted_id)
        return insertion_result

    def do_set_review_status(self, review_id, status):
        assert isinstance(status, ReviewStatus)
        logging.info("Setting status to %s for review %d", status.name, review_id)
        cursor = self.db_conn.cursor()
        query = """
        UPDATE reviews
        SET status = ?
        WHERE review_id = ? AND status != ?;
        """
        cursor.execute(query, (status.name, review_id, ReviewStatus.DISCARDED.name))
        self.db_conn.commit()

    # Returns AddReviewVoteResult
    def do_add_review_vote(self, review_id, user_id, vote):
        approves, disapproves, own_approves, own_disapproves = self.count_review_votes_groupped(user_id, review_id)

        if own_approves + own_disapproves > 0:
            logging.error("do_add_review_vote: %d, %d existing vote(s)", own_approves, own_disapproves)
            return AddReviewVoteResult(False, approves, disapproves, own_approves, own_disapproves, False, "duplicate")

        query = """
        INSERT INTO review_votes (review_id, user_id, vote) VALUES (?, ?, ?);
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (review_id, user_id, vote.name))
        self.db_conn.commit()

        if vote == ReviewVote.APPROVE:
            approves += 1
            own_approves += 1
        else:
            disapproves += 1
            own_disapproves += 1

        gone = False
        if approves > disapproves and approves >= APPROVE_THRESHOLD:
            logging.info("approved: %d approves vs %d disapproves", approves, disapproves)
            self.do_copy_review_to_translations(review_id)
            self.do_set_review_status(review_id, ReviewStatus.APPROVED)
            gone = True
        elif disapproves > approves + 1 and disapproves >= DISAPPROVE_THRESHOLD:
            logging.info("disapproved: %d approves vs %d disapproves", approves, disapproves)
            self.do_set_review_status(review_id, ReviewStatus.DISAPPROVED)
            gone = True

        return AddReviewVoteResult(True, approves, disapproves, own_approves, own_disapproves, gone, None)

    # Returns AddReviewVoteResult
    def add_review_vote(self, review_id, user_id, vote):
        assert isinstance(vote, ReviewVote)
        with self.db_lock:
            return self.do_add_review_vote(review_id, user_id, vote)

    # Returns AddReviewVoteResult
    def do_retract_review_vote(self, review_id, user_id, vote):
        approves, disapproves, own_approves, own_disapproves = self.count_review_votes_groupped(user_id, review_id)

        if (vote == ReviewVote.APPROVE and own_approves <= 0) or (vote == ReviewVote.DISAPPROVE and own_disapproves <= 0):
            logging.error("do_retract_review_vote: no vote %s to retract: %d vs %d", vote.name, own_approves, own_disapproves)
            return AddReviewVoteResult(False, approves, disapproves, own_approves, own_disapproves, False, "not found")

        query = """
        DELETE FROM review_votes WHERE review_id = ? AND user_id = ? AND vote = ?;
        """
        cursor = self.db_conn.cursor()
        cursor.execute(query, (review_id, user_id, vote.name))
        self.db_conn.commit()

        if vote == ReviewVote.APPROVE:
            approves -= 1
            own_approves -= 1
        else:
            disapproves -= 1
            own_disapproves -= 1

        gone = False
        if approves > disapproves:
            self.do_copy_review_to_translations(review_id)
            self.do_set_review_status(review_id, ReviewStatus.APPROVED)
            gone = True
        elif disapproves > approves + 1:
            self.do_set_review_status(review_id, ReviewStatus.DISAPPROVED)

        return AddReviewVoteResult(True, approves, disapproves, own_approves, own_disapproves, gone, None)

    # Returns AddReviewVoteResult
    def retract_review_vote(self, review_id, user_id, vote):
        assert isinstance(vote, ReviewVote)
        with self.db_lock:
            return self.do_retract_review_vote(review_id, user_id, vote)

    # Returns InsertionResult
    def do_discard_review(self, review_id, user_id):
        review = self.do_get_review_by_id(review_id)
        if review is None:
            logging.info("do_discard_review: failed to get a unique review by id")
            return InsertionResult(None, "no review")
        if review["status"] == ReviewStatus.APPROVED.name or review["status"] == ReviewStatus.DISCARDED.name:
            logging.info("do_discard_review: unexpected review status %s", review["status"])
            return InsertionResult(None, "no review")
        if review["user_id"] != user_id:
            logging.info("do_discard_review: user_id does not match: %d vs %d", review["user_id"], user_id)
            return InsertionResult(None, "no review")
        self.do_set_review_status(review_id, ReviewStatus.DISCARDED)
        return InsertionResult(review_id, None)

    # Returns InsertionResult
    def discard_review(self, review_id, user_id):
        with self.db_lock:
            return self.do_discard_review(review_id, user_id)

    def do_extract_feed(self):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                t.translation_id AS tr_id,
                u.name AS name,
                w1.word AS src_word,
                w1.lang AS src_lang,
                w2.word AS dst_word,
                w2.lang AS dst_lang,
                tv.vote AS vote,
                u2.name AS voter,
                strftime('%s', t.created_at) AS created_at
            FROM
                translations t
            JOIN
                users u ON t.user_id = u.user_id
            JOIN
                words w1 ON t.word_id = w1.word_id
            JOIN
                words w2 ON t.translated_word_id = w2.word_id
            LEFT JOIN
                translation_votes tv ON t.translation_id = tv.translation_id
            LEFT JOIN
                users u2 ON tv.user_id = u2.user_id
            WHERE t.created_at >= datetime('now', '-2 days')
            ORDER BY t.created_at DESC
            LIMIT 100;
        """)

        fetched_results = cursor.fetchall()
        feed_items = read_feed_items(fetched_results)
        cursor.close()
        return feed_items

    def get_feed(self):
        with self.db_lock:
            return self.do_extract_feed()

    def do_get_stats(self):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                COUNT(CASE WHEN en_count > 0 THEN 1 END) AS en_count,
                COUNT(CASE WHEN ru_count > 0 THEN 1 END) AS ru_count
            FROM (
                SELECT
                    t.word_id AS word_id,
                    COUNT(CASE WHEN w2.lang = "en" THEN 1 END) AS en_count,
                    COUNT(CASE WHEN w2.lang = "ru" THEN 1 END) AS ru_count
                FROM
                    translations t
                JOIN
                    words w2
                ON t.translated_word_id = w2.word_id
                GROUP BY
                    t.word_id
            );
        """)

        results = cursor.fetchall()

        stats = [
            {
                "en_count": row["en_count"],
                "ru_count": row["ru_count"],
            }
            for row in results
        ]
        cursor.close()
        if len(stats) != 1:
            return None

        return stats[0]

    def get_stats(self):
        cached = self.cache.get_stats()
        if cached:
            return cached
        logging.info("get_stats: No valid cache entry, retrieving from DB")
        with self.db_lock:
            stats = self.do_get_stats()
            if stats:
                self.cache.update_stats(stats)
            return stats

    def do_get_downloads(self):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                id,
                url,
                kkru,
                kken
            FROM
                downloads
            ORDER BY id DESC
            LIMIT 0, 10;
        """)

        results = cursor.fetchall()

        downloads = [
            {
                "id": row["id"],
                "url": row["url"],
                "kkru": row["kkru"],
                "kken": row["kken"],
            }
            for row in results
        ]
        cursor.close()
        return downloads

    def get_downloads(self):
        with self.db_lock:
            downloads = self.do_get_downloads()
            return downloads

    def do_get_untranslated(self, dst_lang):
        result = []

        query = """
            SELECT
                w1.word_id AS source_word_id,
                w1.word AS source_word,
                GROUP_CONCAT(w2.lang) AS translation_langs
            FROM
                words w1
            LEFT JOIN
                translations t ON w1.word_id = t.word_id
            LEFT JOIN
                words w2 ON t.translated_word_id = w2.word_id
            WHERE
                w1.lang = "kk"
            GROUP BY w1.word_id
            LIMIT ?,50;
        """

        cursor = self.db_conn.cursor()
        for iter in range(6):
            offset = random.randint(0, 14200)
            cursor.execute(query, (offset,))
            fetched_results = cursor.fetchall()
            for row in fetched_results:
                translation_langs = row["translation_langs"]
                if translation_langs is None or dst_lang not in translation_langs.split(","):
                    result.append((row["source_word_id"], row["source_word"]))
            if len(result) >= 100:
                break

        return result

    def get_untranslated(self, dst_lang):
        picked = self.untranslated_cache.pick_random(3)
        if picked is None:
            with self.db_lock:
                new_untranslated = self.do_get_untranslated(dst_lang)
                random.shuffle(new_untranslated)
            logging.info("Populating untranslated cache with %d words", len(new_untranslated))
            self.untranslated_cache.reset(new_untranslated)
            picked = self.untranslated_cache.pick_random(3)
        return picked

    def do_get_gpt4omini_translations(self, word_id):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                translations
            FROM
                gpt4omini
            WHERE
                word_id = ?
            LIMIT 1;
        """, (word_id,))
        fetched_results = cursor.fetchall()
        result = []
        for row in fetched_results:
            result.extend(row["translations"].split("\n"))
        cursor.close()
        return result

    def get_llm_translations(self, word_id, model):
        if model == "gpt-4o-mini":
            with self.db_lock:
                return self.do_get_gpt4omini_translations(word_id)
        else:
            logging.error("Unknown model %s for translations", model)
            return None

    def do_get_verb_form_examples(self, verb, fe, neg):
        cursor = self.db_conn.cursor()
        cursor.execute("""
            SELECT
                form, example
            FROM
                verb_form_examples
            WHERE
                verb = ? AND
                fe = ? AND
                neg = ?
            LIMIT 1000;
        """, (verb, fe, neg))
        fetched_results = cursor.fetchall()

        verb_form_examples = {
            row["form"]: row["example"]
            for row in fetched_results
        }
        cursor.close()
        return verb_form_examples

    def get_verb_form_examples(self, verb, fe, neg):
        with self.db_lock:
            return self.do_get_verb_form_examples(verb, fe, neg)


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
CREATE TABLE IF NOT EXISTS translation_votes (
    translation_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    vote TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (translation_id, user_id)
)
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS reviews (
    review_id INTEGER PRIMARY KEY,
    word_id INTEGER NOT NULL,
    translated_word_id INTEGER NOT NULL,
    reference TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS review_votes (
    review_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    vote TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id, user_id)
)
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

    conn.execute("""
CREATE TABLE IF NOT EXISTS gpt4omini (
    word_id INTEGER PRIMARY KEY,
    translations TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())

    conn.execute("""
CREATE TABLE IF NOT EXISTS downloads (
    id INTEGER PRIMARY KEY,
    url TEXT NOT NULL,
    kkru INTEGER NOT NULL,
    kken INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip());

    conn.execute("""
CREATE TABLE IF NOT EXISTS verb_form_examples (
    verb TEXT NOT NULL,
    fe BOOLEAN NOT NULL,
    neg BOOLEAN NOT NULL,
    form TEXT NOT NULL,
    example TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (verb, fe, neg, form)
);
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
    both_dirs = request.args.get("both") == "1"
    word = request.args.get("w")

    logging.info("Request /get_translation %s->%s, both dirs %s: %s", src_lang, dst_lang, str(both_dirs), word)

    if not valid_lang(src_lang):
        logging.error("Invalid src")
        return jsonify({"message": "Invalid request"}), 400
    if not valid_lang(dst_lang):
        logging.error("Invalid dst")
        return jsonify({"message": "Invalid request"}), 400
    if word is None or not (0 < len(word) < 64):
        logging.error("Invalid word")
        return jsonify({"message": "Invalid request"}), 400

    translations = gc_instance.get_translations(src_lang, dst_lang, both_dirs, word)
    return jsonify({"translations": translations}), 200


@app.route("/gcapi/v1/get_translation_info", methods=["GET"])
def get_translation_info():
    global gc_instance

    translation_id_str = request.args.get("tid")

    logging.info("Request /get_translation_info %s", translation_id_str)

    if not translation_id_str or not translation_id_str.isdigit():
        logging.error("Invalid translation_id")
        return jsonify({"message": "Invalid request"}), 400

    translation_id = int(translation_id_str)

    translation_info = gc_instance.get_translation_info(translation_id)
    return jsonify({"translation_info": translation_info}), 200



@app.route("/gcapi/v1/get_words", methods=["GET"])
def get_words():
    global gc_instance

    word = request.args.get("w")
    lang = request.args.get("lang")
    with_translations = request.args.get("wtrs") == "1"

    if not word:
        logging.error("Invalid word")
        return jsonify({"message": "Invalid word"}), 400
    if not validate_lang(lang):
        logging.error("Invalid language")
        return jsonify({"message": "Invalid language"}), 400

    logging.info("Request /get_words %s, lang %s, with_translations %s", word, lang, str(with_translations))

    words = gc_instance.get_words(word, lang, with_translations)
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
    inserted_id = insertion_result.inserted_id
    if inserted_id is None:
        logging.error("No inserted_id after insertion: %s", insertion_result.error_message)
        return jsonify({"message": insertion_result.error_message}), 500
    return jsonify({"message": "ok", "translation_id": inserted_id}), 201


@app.route("/gcapi/v1/add_review", methods=["POST"])
def post_add_review():
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

    insertion_result = gc_instance.add_review(
        int(src_id),
        int(dst_id),
        reference,
        user_id,
    )
    if insertion_result is None:
        logging.error("No insertion_result after insertion")
        return jsonify({"message": "Internal error"}), 500
    inserted_id = insertion_result.inserted_id
    if inserted_id is None:
        logging.error("No inserted_id after insertion: %s", insertion_result.error_message)
        return jsonify({"message": insertion_result.error_message}), 500
    return jsonify({"message": "ok", "review_id": inserted_id}), 201


@app.route("/gcapi/v1/get_reviews", methods=["GET"])
def get_reviews():
    global gc_instance

    user_id = gc_instance.get_user_id_from_header(request.headers)
    if not user_id:
        user_id = 0

    src_lang = request.args.get("src")
    dst_lang = request.args.get("dst")
    approves_min = request.args.get("am")
    page_raw = request.args.get("p")
    page = int(page_raw) if (isinstance(page_raw, str) and page_raw.isdigit()) else 0

    if not (src_lang is None and dst_lang is None):
        if not valid_lang(src_lang):
            logging.error("Invalid src lang")
            return jsonify({"message": "Invalid request"}), 400
        if not valid_lang(dst_lang):
            logging.error("Invalid dst lang")
            return jsonify({"message": "Invalid request"}), 400
        if not (approves_min is None):
            logging.error("Incompatible arguments: am and src/dst")
            return jsonify({"message": "Invalid request"}), 400
        if src_lang == dst_lang:
            logging.error("Invalid combination of src and dst lang")
            return jsonify({"message": "Invalid request"}), 400
        reviews = gc_instance.get_reviews_by_dir(user_id, src_lang, dst_lang, page)
    else:
        if approves_min is None:
            approves_min_arg = 0
        elif approves_min.isdigit():
            approves_min_arg = int(approves_min)
        else:
            logging.error("Invalid value for am: %s", str(approves_min))
            return jsonify({"message": "Invalid request"}), 400
        assert isinstance(approves_min_arg, int), f"bad type: {type(approves_min_arg)}"
        reviews = gc_instance.get_reviews(user_id, approves_min_arg, page)

    if reviews is None:
        logging.error("null reviews")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "reviews": reviews}), 200


@app.route("/gcapi/v1/add_review_vote", methods=["POST"])
def post_add_review_vote():
    global gc_instance

    user_id = gc_instance.get_user_id_from_header(request.headers)
    if not user_id:
        return jsonify({"message": "Unauthorized"}), 401

    request_data = request.json
    review_id = request_data.get("rid")
    v = request_data.get("v")

    if not isinstance(review_id, int):
        logging.error("Invalid review_id: %s", str(review_id))
        return jsonify({"message": "Invalid review"}), 400

    try:
        vote = ReviewVote[v]
    except KeyError as e:
        logging.error("Invalid vote: got KeyError %s", str(e))
        return jsonify({"message": "Invalid vote"}), 400

    result = gc_instance.add_review_vote(review_id, user_id, vote)
    if not result.inserted:
        logging.error("failed to add review vote: %s", result.error_message)
        message = result.error_message if result.error_message == "duplicate" else "Internal error"
        return jsonify(result.make_response(message)), 500
    return jsonify(result.make_response("ok")), 200


@app.route("/gcapi/v1/retract_review_vote", methods=["POST"])
def post_retract_review_vote():
    global gc_instance

    user_id = gc_instance.get_user_id_from_header(request.headers)
    if not user_id:
        return jsonify({"message": "Unauthorized"}), 401

    request_data = request.json
    review_id = request_data.get("rid")
    v = request_data.get("v")

    if not isinstance(review_id, int):
        logging.error("Invalid review_id: %s", str(review_id))
        return jsonify({"message": "Invalid review"}), 400

    try:
        vote = ReviewVote[v]
    except KeyError as e:
        logging.error("Invalid vote: got KeyError %s", str(e))
        return jsonify({"message": "Invalid vote"}), 400

    result = gc_instance.retract_review_vote(review_id, user_id, vote)
    if not result.inserted:
        logging.error("failed to retract review vote: %s", result.error_message)
        message = result.error_message if result.error_message == "not found" else "Internal error"
        return jsonify(result.make_response(message)), 500
    return jsonify(result.make_response("ok")), 200


@app.route("/gcapi/v1/discard_review", methods=["POST"])
def post_discard_review():
    global gc_instance

    user_id = gc_instance.get_user_id_from_header(request.headers)
    if not user_id:
        return jsonify({"message": "Unauthorized"}), 401

    request_data = request.json
    review_id = request_data.get("rid")

    if not isinstance(review_id, int):
        logging.error("Invalid review_id: %s", str(review_id))
        return jsonify({"message": "Invalid review_id"}), 400

    insertion_result = gc_instance.discard_review(
        review_id,
        user_id,
    )
    if insertion_result is None:
        logging.error("post_discard_review: no insertion_result")
        return jsonify({"message": "Internal error"}), 500
    discarded_id = insertion_result.inserted_id
    if discarded_id is None:
        logging.error("post_discard_review: no inserted_id after discard: %s", insertion_result.error_message)
        return jsonify({"message": insertion_result.error_message}), 500
    return jsonify({"message": "ok", "review_id": discarded_id}), 201


@app.route("/gcapi/v1/get_feed", methods=["GET"])
def get_feed():
    global gc_instance

    feed = gc_instance.get_feed()
    if feed is None:
        logging.error("null feed")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "feed": feed}), 200


@app.route("/gcapi/v1/get_stats", methods=["GET"])
def get_stats():
    global gc_instance

    stats = gc_instance.get_stats()
    if stats is None:
        logging.error("null stats")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "stats": stats}), 200


@app.route("/gcapi/v1/get_downloads", methods=["GET"])
def get_downloads():
    global gc_instance

    downloads = gc_instance.get_downloads()
    return jsonify({"message": "ok", "downloads": downloads}), 200


@app.route("/gcapi/v1/get_untranslated", methods=["GET"])
def get_untranslated():
    global gc_instance

    dst_lang = request.args.get("dst")

    if not valid_lang(dst_lang):
        logging.error("Invalid dst lang")
        return jsonify({"message": "Invalid request"}), 400

    words = gc_instance.get_untranslated(dst_lang)
    if words is None:
        logging.error("null untranslated")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "words": words}), 200


@app.route("/gcapi/v1/get_llm_translations", methods=["GET"])
def get_llm_translations():
    global gc_instance

    word_id = request.args.get("wid")
    model = request.args.get("model")

    if not word_id.isdigit():
        logging.error("Invalid word_id: %s", word_id)
        return jsonify({"message": "Invalid word_id"}), 400
    if not isinstance(model, str) or len(model) <= 0:
        logging.error("Invalid model: %s", str(model))
        return jsonify({"message": "Invalid model"}), 400

    translations = gc_instance.get_llm_translations(int(word_id), model)
    if translations is None:
        logging.error("null translations")
        return jsonify({"message": "Internal error"}), 500
    return jsonify({"message": "ok", "translations": translations}), 200


@app.route("/gcapi/v1/get_verb_form_examples", methods=["GET"])
def get_verb_form_examples():
    global gc_instance

    verb = request.args.get("v")
    fe = request.args.get("fe") == "1"
    neg = request.args.get("neg") == "1"

    if not isinstance(verb, str) or len(verb) < 2:
        logging.error("Invalid verb argument: %s", str(verb))
        return jsonify({"message": "Invalid request"}), 400

    verb_form_examples = gc_instance.get_verb_form_examples(verb, fe, neg)
    return jsonify({"message": "ok", "verb_form_examples": verb_form_examples}), 200


def main():
    return 0


if __name__ == '__main__':
    sys.exit(main())
else:
    init_gc_app()
