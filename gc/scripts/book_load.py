import logging
import sqlite3
import sys

DB_PATH = "gc.db"
TABLE_NAME = "book_chunks"
BOOK_ID = 1001


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("""
CREATE TABLE IF NOT EXISTS book_chunks (
    book_id INTEGER NOT NULL,
    chunk_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (book_id, chunk_id)
);
    """.strip())

    logging.info("Database connection with %s established", db_path)
    return conn


def insert_lines(db_conn, lines, offset):
    query = f"INSERT INTO {TABLE_NAME} (book_id, chunk_id, content) VALUES (?, ?, ?);"
    records = []
    for i, line in enumerate(lines):
        records.append((BOOK_ID, offset + i, line))
    cursor = db_conn.cursor()
    cursor.executemany(query, records)
    db_conn.commit()
    return offset + len(lines)


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    db_conn = init_db_conn(DB_PATH)

    offset = 0
    accumulated = []

    for line in sys.stdin:
        if len(line.strip()) == 0:
            continue
        accumulated.append(line.rstrip())
        if len(accumulated) > 10:
            offset = insert_lines(db_conn, accumulated, offset)
            accumulated = []
        assert offset < 10000, f"too many lines: {offset}"
    if len(accumulated) > 0:
        offset = insert_lines(db_conn, accumulated, offset)
    logging.info("Loaded %d book chunks", offset)


main()