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


# Returns list with word_ids:
# [
#    (
#       [duplicate_to_keep, duplicate_to_keep, ...],
#       [duplicate_to_drop, duplicate_to_drop, ...],
#    ),
#    ...
# ]
#
def load_duplicated_ru_words(db_conn):
    cursor = db_conn.cursor()
    cursor.execute("""
SELECT
    GROUP_CONCAT(w.word_id) AS word_ids,
    GROUP_CONCAT(COALESCE(t.translation_id, "")) AS translation_ids
FROM words w
LEFT JOIN translations t ON w.word_id = t.translated_word_id
WHERE w.lang = "ru"
GROUP BY w.word, w.pos, w.exc_verb, w.comment, w.lang
HAVING MIN(w.word_id) < MAX(w.word_id);
    """)
    fetched = cursor.fetchall()
    result = []
    for row in fetched:
        word_ids = row["word_ids"].split(",")
        translation_ids = row["translation_ids"].split(",")
        assert len(word_ids) == len(translation_ids)
        to_keep = []
        to_drop = []
        for i in range(len(word_ids)):
            if len(translation_ids[i]) > 0:
                to_keep.append(word_ids[i])
            else:
                to_drop.append(word_ids[i])
        if len(to_keep) == 0:
            to_keep.append(to_drop[0])
            to_drop = to_drop[1:]
        assert len(to_keep) > 0
        assert len(to_drop) > 0
        result.append((to_keep, to_drop))
    logging.info("Loaded %d groups of duplicated words", len(result))
    return result


def move_duplicated_ru_words(db_conn, dupgroups):
    db_conn.execute("""
CREATE TABLE IF NOT EXISTS duplicated_words (
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
    word_ids = []
    for dupgroup in dupgroups:
        word_ids.extend(dupgroup[1])
    part_size = 50
    copied = 0
    for offset in range(0, len(word_ids), part_size):
        group_str = word_ids[offset:min(len(word_ids), offset + part_size)]
        group = [int(word_id) for word_id in group_str]
        query = f"INSERT INTO duplicated_words SELECT word_id, word, pos, exc_verb, comment, user_id, lang, created_at FROM words WHERE word_id IN {tuple(group)};"
        logging.debug("Q: %s", query)
        db_conn.execute(query)
        db_conn.commit()
        copied += len(group)
    logging.info("Copied %d words to duplicated_words", copied)
    deleted = 0
    for offset in range(0, len(word_ids), part_size):
        group_str = word_ids[offset:min(len(word_ids), offset + part_size)]
        group = [int(word_id) for word_id in group_str]
        query = f"DELETE FROM words WHERE word_id IN {tuple(group)};"
        logging.debug("Q: %s", query)
        db_conn.execute(query)
        db_conn.commit()
        deleted += len(group)
    logging.info("Deleted %d words", deleted)


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


# Returns a list of integer tuples: [(word_id, translated_word_id)]
def load_matches(db_conn, offset, limit):
    cursor = db_conn.cursor()
    cursor.execute(f"""
SELECT
    kk.word_id AS word_id,
    ru.word_id AS translated_word_id
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
LIMIT {offset},{limit};
    """)
    fetched = cursor.fetchall()
    result = []
    for row in fetched:
        result.append((row["word_id"], row["translated_word_id"]))
    cursor.close()
    return result


# Returns a set of integer tuples: {(word_id, translated_word_id)}
def load_existing_translations(db_conn):
    cursor = db_conn.cursor()
    cursor.execute("""
SELECT
    word_id,
    translated_word_id
FROM translations;
    """)
    fetched = cursor.fetchall()
    result = set()
    for row in fetched:
        result.add((row["word_id"], row["translated_word_id"]))
    cursor.close()
    logging.info("Loaded %d existing translations", len(result))
    return result


def make_reviews_insert(matches):
    prefix = "INSERT INTO reviews (word_id, translated_word_id, reference, user_id, status) VALUES\n"
    rows = []
    for word_id, translated_word_id in matches:
        rows.append(f"  ({word_id}, {translated_word_id}, 'ru.wiktionary.org', 8, 'NEW')")
    query = prefix + ",\n".join(rows) + ";"
    return query


def make_reviews_from_matches(db_conn, matches_count, existing_translations, output):
    part_size = 50
    for offset in range(0, matches_count, part_size):
        matches = load_matches(db_conn, offset, part_size)
        assert len(matches) > 0
        filtered = [m for m in matches if m not in existing_translations]
        filtered_out = len(matches) - len(filtered)
        if filtered_out > 0:
            logging.info("Filtered out %d matches out of %d due to existing translations", filtered_out, len(matches))
        if len(filtered) > 0:
            query = make_reviews_insert(filtered)
            output.write(f"{query}\n\n")


def make_reviews(args):
    db_conn = init_db_conn(args.db_path)

    dupgroups = load_duplicated_ru_words(db_conn)
    if len(dupgroups):
        move_duplicated_ru_words(db_conn, dupgroups)

    logging.info("Creating DB table: wikt_kk_words")
    db_conn.execute("""
CREATE TABLE IF NOT EXISTS wikt_kk_words (
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
    db_conn.commit()

    logging.info("Creating DB table: wikt_ru_words")
    db_conn.execute("""
CREATE TABLE IF NOT EXISTS wikt_ru_words (
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
    db_conn.commit()

    matches_count = count_matches(db_conn)
    logging.info("Found %d matches", matches_count)

    existing_translations = load_existing_translations(db_conn)
    with open(args.sql, "wt") as output:
        make_reviews_from_matches(db_conn, matches_count, existing_translations, output)


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