import argparse
import logging
from lxml import etree
import re
import sqlite3
import sys

KK_WORD_PATTERN = re.compile("^[А-Яа-я-'ЁӘІҢҒҮҰҚӨҺёәіңғүұқөһ/ ]+$")
AS_RU_TAG = "as ru"
DATABASE_PATH = "gc.db"


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    logging.info("Database connection with %s established", db_path)
    return conn


def validate_word(word):
    return "\t" not in word and "\n" not in word


def valid_kk_word(word):
    return KK_WORD_PATTERN.match(word)


# Returns (cleaned translation, comment) or None
def clean_ru_gloss(ctx, s):
    orig = s
    comment = []
    while True:
        braces_start = s.find("{{")
        if braces_start < 0:
            break

        braces_end = -1
        i = braces_start + 2
        n = len(s)
        balance = 1
        open_counter = 0
        close_counter = 0
        while i < n:
            c = s[i]
            if c == "{":
                open_counter += 1
                close_counter = 0
                if open_counter == 2:
                    balance += 1
                    open_counter = 0
            elif c == '}':
                open_counter = 0
                close_counter += 1
                if close_counter == 2:
                    balance -= 1
                    if balance == 0:
                        braces_end = i + 1
                        break
                    close_counter = 0
            else:
                open_counter = 0
                close_counter = 0
            i += 1

        if braces_end <= braces_start:
            logging.warning("Dropping malformed translation [%s], context [%s]", orig, ctx)
            return None
        comment.append(s[braces_start + 2:braces_end - 2])

        remove_start = braces_start
        while remove_start - 1 >= 0 and s[remove_start - 1] == " ":
            remove_start -= 1
        remove_end = braces_end
        while remove_end < len(s) and s[remove_end] == " ":
            remove_end += 1
        s = f"{s[:remove_start]}{s[remove_end:]}"

    if s.find("}}") >= 0 or s.find("{{") >= 0:
        logging.warning("Dropping malformed translation [%s], context [%s]", orig, ctx)
        return None

    if s.startswith(" ") or s.endswith(" "):
        logging.warning("Dropping translation [%s] with hanging whitespaces, context [%s]", orig, ctx)
        return None

    if len(s) == 0:
        try:
            as_ru_pos = comment.index(AS_RU_TAG)
        except ValueError as exc:
            as_ru_pos = -1
        if as_ru_pos < 0:
            logging.warning("Dropping empty translation [%s], context [%s]", orig, ctx)
            return None
        newcomment = comment[:as_ru_pos] + comment[as_ru_pos + 1:]
        logging.debug("Modifying comment: %s => %s", str(comment), str(newcomment))
        comment = newcomment
        s = ctx

    if len(comment) > 1:
        logging.warning("Dropping translation with too many comments [%s], context [%s]", orig, ctx)
        return None

    return s, "".join(comment)


def parse_tei_generator(tei):
    tree = etree.parse(tei)

    namespaces = {'x': 'http://www.tei-c.org/ns/1.0'}
    for entry in tree.xpath('//x:entry', namespaces=namespaces):
        orth = entry.xpath('x:form/x:orth', namespaces=namespaces)[0]
        word = orth.text
        if not valid_kk_word(word):
            logging.error("invalid kk word: [ %s ]", word)
            continue
        assert validate_word(word)
        quotes = entry.xpath(
            'x:sense/x:cit[@type="trans"]/x:quote',
            namespaces=namespaces
        )
        for quote in quotes:
            translation_comment = clean_ru_gloss(word, quote.text)
            if translation_comment is None:
                continue
            translation, comment = translation_comment
            assert validate_word(translation)
            assert validate_word(comment)
            yield word, translation, comment


def parse_tei_to_file(tei, output):
    for word, translation, comment in parse_tei_generator(tei):
        output.write(f"{word}\t{translation}\t{comment}\n")


def parse_tei(args):
    with open(args.tsv, "wt") as output:
        parse_tei_to_file(args.tei, output)


def import_tei(args):
    db_conn = init_db_conn(args.db_path)

    db_conn.execute("""
CREATE TABLE IF NOT EXISTS wikt_rukk (
    translation_id INTEGER PRIMARY KEY,
    kk_word TEXT NOT NULL,
    ru_word TEXT NOT NULL,
    comment TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())
    cursor = db_conn.cursor()
    total = 0

    def flush(batch):
        nonlocal cursor, db_conn, total

        if len(batch) > 0:
            cursor.executemany("INSERT INTO wikt_rukk (kk_word, ru_word, comment) VALUES (?, ?, ?)", batch)
            db_conn.commit()
            logging.info("Inserted %d rows into DB", len(batch))
            total += len(batch)

    batch = []

    def add_to_batch(kk_word, ru_word, comment):
        nonlocal batch
        batch.append((kk_word, ru_word, comment))
        if len(batch) >= 100:
            flush(batch)
            batch = []

    for word, translation, comment in parse_tei_generator(args.tei):
        add_to_batch(word, translation, comment)

    flush(batch)
    cursor.close()
    logging.info("Inserted %d rows total into DB", total)


def count_matches(db_conn):
    cursor = db_conn.cursor()
    cursor.execute("""
SELECT
    COUNT(*)
FROM
    wikt_kk_words AS kk
JOIN
    wikt_ru_words AS ru
ON kk.translation_id = ru.translation_id
JOIN
    words AS wkk
ON kk.word_id = wkk.word_id
JOIN
    words AS wru
ON ru.word_id = wru.word_id
WHERE
    wkk.pos = wru.pos
    """)
    count = cursor.fetchone()[0]
    cursor.close()
    return count


def make_reviews(args):
    db_conn = init_db_conn(args.db_path)

    logging.info("Creating DB table: wikt_kk_words")
    db_conn.execute("""
CREATE TABLE wikt_kk_words (
    translation_id INTEGER NOT NULL,
    word_id INTEGER NOT NULL,
    PRIMARY KEY (translation_id, word_id)
);
    """.strip())

    logging.info("Populating DB table: wikt_kk_words")
    db_conn.execute("""
INSERT INTO wikt_kk_words
SELECT
    wi.translation_id AS translation_id,
    w.word_id AS word_id
FROM wikt_rukk wi
JOIN
    words w
ON wi.kk_word = w.word
WHERE w.lang = "kk";
    """.strip())

    logging.info("Creating DB table: wikt_ru_words")
    db_conn.execute("""
CREATE TABLE wikt_ru_words (
    translation_id INTEGER NOT NULL,
    word_id INTEGER NOT NULL,
    PRIMARY KEY (translation_id, word_id)
);
    """.strip())

    logging.info("Populating DB table: wikt_ru_words")
    db_conn.execute("""
INSERT INTO wikt_ru_words
SELECT
    wi.translation_id AS translation_id,
    w.word_id AS word_id
FROM wikt_rukk wi
JOIN
    words w
ON wi.ru_word = w.word
WHERE w.lang = "ru";
    """.strip())

    matches = count_matches(db_conn)
    logging.info("Found %d matches", matches)
    # TODO produce INSERT statements for all matches, filter them by existing translations


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()
    parse_tei_parser = subparsers.add_parser("parse-tei")
    parse_tei_parser.add_argument("--tei", required=True, help="Path to an input *.tei file")
    parse_tei_parser.add_argument("--tsv", required=True, help="Path to an output *.tsv file")
    parse_tei_parser.set_defaults(func=parse_tei)

    import_tei_parser = subparsers.add_parser("import-tei")
    import_tei_parser.add_argument("--tei", required=True, help="Path to an input *.tei file")
    import_tei_parser.add_argument("--db-path", default=DATABASE_PATH)
    import_tei_parser.set_defaults(func=import_tei)

    make_reviews_parser = subparsers.add_parser("make-reviews")
    make_reviews_parser.add_argument("--db-path", default=DATABASE_PATH)
    make_reviews_parser.add_argument("--sql", required=True, help="Path to an output *.sql file")
    make_reviews_parser.set_defaults(func=make_reviews)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())