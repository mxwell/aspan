"""
Usage:

Prerequisites:
- directory `gen'
- SQLite DB file `gc.db` in the workdir
- pip install openai
- create and export an API key: `export OPENAI_API_KEY="your_api_key_here"`

Then the work is done in multiple batches, with the following steps per batch:

1. Declare offset

`export OFFSET=200`

2. Select words from DB and prepare a JSON lines files with prompt jobs

```
$ python3 scripts/gpt_translate.py make-batch --offset $OFFSET --count 100 --json gen/b${OFFSET}.jsonl --words gen/w${OFFSET}.jsonl
Database connection with gc.db established
Loaded 100 word(s) from DB
Batch JSON lines are saved to gen/b200.jsonl
Words are saved to gen/w200.jsonl
```

3. Launch batch processing

```
$ python3 scripts/gpt_translate.py launch-batch --jsonl gen/b${OFFSET}.jsonl
...
Batch ID: batch_0A4g70JTMTOzbGSplJOB8NaW
...
```

4. Wait for the batch to complete in https://platform.openai.com/batches

5. If it has completed all right, dump the result and join with the input words.

NOTE:Don't forget to replace the placeholder with your actual batch ID.

```
$ python3 scripts/gpt_translate.py get-batch-result --words gen/w${OFFSET}.jsonl --batch-id "your_batch_id_here" --raw gen/r${OFFSET}.jsonl --translations gen/t${OFFSET}.jsonl
...
Raw batch result is saved to gen/r200.jsonl
...
Translations are saved to gen/t200.jsonl
```

5. Insert translations into DB.

```
$ python3 scripts/gpt_translate.py insert-translations --words gen/w${OFFSET}.jsonl --raw gen/r${OFFSET}.jsonl
...
Inserted 92 rows total into DB
```

"""

import argparse
from dataclasses import dataclass
import json
import logging
import sqlite3
import sys

from openai import OpenAI

DATABASE_PATH = "gc.db"
POS_NAMES = {
    "ADJ": "прилагательное",
    "ADV": "наречие",
    "NOUN": "существительное",
    "VERB": "глагол",
}


def init_db_conn(db_path):
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    logging.info("Database connection with %s established", db_path)
    return conn


@dataclass
class WordInfo:
    word_id: int
    word: str
    pos: str
    pos_ru: str

    def as_dict(self):
        return {
            "word_id": self.word_id,
            "word": self.word,
            "pos": self.pos,
            "pos_ru": self.pos_ru
        }


def load_words(db_conn, offset, count):
    cursor = db_conn.cursor()
    cursor.execute("""
        SELECT
            word_id,
            word,
            pos
        FROM words
        WHERE lang = "kk" AND pos IN ("ADJ", "ADV", "NOUN", "VERB")
        LIMIT ?, ?;
    """, (offset, count))
    fetched_results = cursor.fetchall()

    used = set()
    words = []
    for row in fetched_results:
        word_id = row["word_id"]
        assert isinstance(word_id, int) and word_id > 0
        word = row["word"]
        assert isinstance(word, str) and (0 < len(word) < 64)
        pos = row["pos"]
        pos_ru = POS_NAMES[pos]
        assert len(pos_ru) > 3
        key = (word, pos)
        if key in used:
            continue
        used.add(key)
        words.append(WordInfo(word_id, word, pos, pos_ru))
    logging.info("Loaded %d word(s) from DB", len(words))
    return words


def build_prompt(word, pos_ru):
    return f"""Ты помощник составителя казахско-русского словаря. Ниже тебе будет дано казахское слово. Ты должен выдать список переводов на русский. Перевод - это одно слово или фраза и пример использования. Будь краток, введение и пояснения к ответу не нужны. Если тебе не известны переводы слова, напиши "Нет перевода".

Ожидаемый формат ответа на примере слова "қазір" (наречие):
- сейчас: қазір дайын емеспін = я сейчас еще не готов
- теперь: қазір жан бағу оңай емес = теперь жить нелегко

Твоя задача - слово "{word}" ({pos_ru})"""


def make_batch(args):
    assert args.count <= 2000

    db_conn = init_db_conn(args.db_path)
    words = load_words(db_conn, args.offset, args.count)
    with open(args.jsonl, "wt") as jsonl_file:
        with open(args.words, "wt") as words_file:
            for word in words:
                obj = {
                    "custom_id": str(word.word_id),
                    "method": "POST",
                    "url": "/v1/chat/completions",
                    "body": {
                        "model": "gpt-4o-mini",
                        "messages": [
                            {
                                "role": "system",
                                "content": "You are a helpful assistant."
                            },
                            {
                                "role": "user",
                                "content": build_prompt(word.word, word.pos_ru)
                            }
                        ],
                        "max_tokens": 1000
                    }
                }
                jsonl_file.write(f"{json.dumps(obj)}\n")
                words_file.write(f"{json.dumps(word.as_dict())}\n")
    logging.info("Batch JSON lines are saved to %s", args.jsonl)
    logging.info("Words are saved to %s", args.words)


def launch_batch(args):
    assert args.jsonl.endswith(".jsonl")

    client = OpenAI()
    batch_input_file = client.files.create(
        file=open(args.jsonl, "rb"),
        purpose="batch"
    )
    batch_input_file_id = batch_input_file.id
    logging.info("File ID: %s", batch_input_file_id)

    batch_object = client.batches.create(
        input_file_id=batch_input_file_id,
        endpoint="/v1/chat/completions",
        completion_window="24h",
        metadata={
            "description": "batch translation"
        }
    )
    # logging.info("Batch object: %s", str(batch_object))
    logging.info("Batch ID: %s", batch_object.id)
    status = client.batches.retrieve(batch_object.id)
    logging.info("Batch status: %s", status.status)
    print(f"Batch ID again:\n\n    {batch_object.id}\n")


def reload_words(words_filepath):
    words = dict()
    for line in open(words_filepath):
        j = json.loads(line)
        word = WordInfo(
            j["word_id"],
            j["word"],
            j["pos"],
            j["pos_ru"]
        )
        words[str(word.word_id)] = word
    return words


def extract_response(j):
    custom_id = j["custom_id"]
    response = j["response"]
    status_code = response["status_code"]
    if status_code != 200:
        logging.error("status_code %d for custom_id %s", status_code, custom_id)
        return custom_id, None
    content = response["body"]["choices"][0]["message"]["content"]
    return custom_id, content


def iscyr(c):
    return u'\u0400' <= c <=u'\u04FF' or u'\u0500' <= c <= u'\u052F'


def valid_ru_translation(w):
    cyr = len([c for c in w if iscyr(c)])
    return cyr * 2 >= len(w)


def extract_translations(content):
    translations = []
    for line in content.split("\n"):
        if line.startswith("Нет перевода"):
            return []
        if line.startswith("нет перевода"):
            return []
        if not line.startswith("- "):
            logging.error("invalid format, bad prefix: %s", line)
            continue
        colon = line.find(": ", 3)
        if colon < 0:
            logging.error("invalid format, no colon: %s", line)
            continue
        w = line[2:colon]
        if not valid_ru_translation(w):
            logging.error("invalid translation: %s", line)
        translations.append(line[2:].strip())
    return translations


def get_batch_result(args):
    word_by_id = reload_words(args.words)

    client = OpenAI()
    batch_object = client.batches.retrieve(args.batch_id)
    logging.info("Batch output file ID: %s", batch_object.output_file_id)
    file_response = client.files.content(batch_object.output_file_id)
    with open(args.raw, "w") as raw_file:
        raw_file.write(file_response.text)
    logging.info("Raw batch result is saved to %s", args.raw)
    lines = file_response.text.split("\n")
    with open(args.translations, "w") as translations_file:
        for rawline in lines:
            line = rawline.strip()
            if len(line) == 0:
                continue
            j = json.loads(line)
            custom_id, response = extract_response(j)
            if response is None:
                continue
            if custom_id not in word_by_id:
                logging.error("custom_id %s is not found in words", custom_id)
                continue
            word = word_by_id[custom_id]
            translations = extract_translations(response)
            if len(translations) == 0:
                logging.error("no translations for custom_id %s, word %s", custom_id, word.word)
                continue
            d = word.as_dict()
            d["translations"] = translations
            translations_file.write(f"{json.dumps(d)}\n")
    logging.info("Translations are saved to %s", args.translations)


def insert_translations(args):
    word_by_id = reload_words(args.words)

    db_conn = init_db_conn(args.db_path)

    db_conn.execute("""
CREATE TABLE IF NOT EXISTS gpt4omini (
    word_id INTEGER PRIMARY KEY,
    translations TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
    """.strip())
    cursor = db_conn.cursor()
    total = 0

    def flush(batch):
        nonlocal cursor, db_conn, total

        if len(batch) > 0:
            cursor.executemany("INSERT INTO gpt4omini (word_id, translations) VALUES (?, ?)", batch)
            db_conn.commit()
            logging.info("Inserted %d rows into DB", len(batch))
            total += len(batch)

    batch = []

    def add_to_batch(word_id, translations):
        nonlocal batch
        batch.append((word_id, translations))
        if len(batch) >= 100:
            flush(batch)
            batch = []

    for line in open(args.raw):
        j = json.loads(line)
        custom_id, response = extract_response(j)
        if response is None:
            continue
        if custom_id not in word_by_id:
            logging.error("custom_id %s is not found in words", custom_id)
            continue
        word = word_by_id[custom_id]
        translations = extract_translations(response)
        if len(translations) == 0:
            logging.error("no translations for custom_id %s, word %s", custom_id, word.word)
            continue
        add_to_batch(int(custom_id), "\n".join(translations))

    flush(batch)
    logging.info("Inserted %d rows total into DB", total)


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    make_batch_parser = subparsers.add_parser("make-batch")
    make_batch_parser.add_argument("--offset", type=int, required=True)
    make_batch_parser.add_argument("--count", type=int, required=True)
    make_batch_parser.add_argument("--db-path", default=DATABASE_PATH)
    make_batch_parser.add_argument("--jsonl", required=True)
    make_batch_parser.add_argument("--words", required=True)
    make_batch_parser.set_defaults(func=make_batch)

    launch_batch_parser = subparsers.add_parser("launch-batch")
    launch_batch_parser.add_argument("--jsonl", required=True)
    launch_batch_parser.set_defaults(func=launch_batch)

    get_batch_result_parser = subparsers.add_parser("get-batch-result")
    get_batch_result_parser.add_argument("--words", required=True)
    get_batch_result_parser.add_argument("--batch-id", required=True)
    get_batch_result_parser.add_argument("--raw", required=True)
    get_batch_result_parser.add_argument("--translations", required=True)
    get_batch_result_parser.set_defaults(func=get_batch_result)

    insert_translations_parser = subparsers.add_parser("insert-translations")
    insert_translations_parser.add_argument("--words", required=True)
    insert_translations_parser.add_argument("--raw", required=True)
    insert_translations_parser.add_argument("--db-path", default=DATABASE_PATH)
    insert_translations_parser.set_defaults(func=insert_translations)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())