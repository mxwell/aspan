import argparse
from dataclasses import dataclass
import json
import logging
import os
import sys
from typing import List

from anthropic import Anthropic
import google.generativeai as genai
from openai import OpenAI

MAX_TOKENS = 1000


@dataclass
class InputWord:
    word: str
    pos_ru: str
    translations: List[str]


def build_prompt(input_word):
    return f"""Ты помощник составителя казахско-русского словаря. Ниже тебе будет дано казахское слово. Ты должен выдать список переводов на русский. Перевод - это одно слово или фраза и пример использования. Будь краток, введение и пояснения к ответу не нужны. Если тебе не известны переводы слова, напиши "Нет перевода".

Ожидаемый формат ответа на примере слова "қазір" (наречие):
- сейчас: қазір дайын емеспін = я сейчас еще не готов
- теперь: қазір жан бағу оңай емес = теперь жить нелегко

Твоя задача - слово "{input_word.word}" ({input_word.pos_ru})"""


def load_word_set(path):
    result = []
    for line in open(path):
        parts = line.strip().split("\t")
        assert len(parts) == 3, f"invalid line: {line}"
        translations = parts[2].split(",")
        result.append(InputWord(parts[0], parts[1], translations))
    return result


def validate_openai_model(model):
    assert model in ["gpt-4o-mini-2024-07-18", "gpt-4o-2024-08-06"]


def launch_batch(args):
    word_set = load_word_set(args.input_words)
    assert len(word_set) >= 10
    validate_openai_model(args.model)

    with open(args.temp_batch, "wt") as jsonl_file:
        for index, input_word in enumerate(word_set):
            obj = {
                "custom_id": str(index),
                "method": "POST",
                "url": "/v1/chat/completions",
                "body": {
                    "model": args.model,
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are a helpful assistant."
                        },
                        {
                            "role": "user",
                            "content": build_prompt(input_word)
                        }
                    ],
                    "max_tokens": MAX_TOKENS
                }
            }
            jsonl_file.write(f"{json.dumps(obj)}\n")
    logging.info("Batch JSON lines are saved to %s", args.temp_batch)

    client = OpenAI()
    batch_input_file = client.files.create(
        file=open(args.temp_batch, "rb"),
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
    logging.info("Batch ID: %s", batch_object.id)
    status = client.batches.retrieve(batch_object.id)
    logging.info("Batch status: %s", status.status)
    print(f"Batch ID again:\n\n    {batch_object.id}\n")
    with open(args.batch_id_path, "wt") as outfile:
        outfile.write(f"{batch_object.id}")
    return 0


def extract_response(j):
    custom_id = j["custom_id"]
    response = j["response"]
    status_code = response["status_code"]
    if status_code != 200:
        logging.error("status_code %d for custom_id %s", status_code, custom_id)
        return custom_id, None
    content = response["body"]["choices"][0]["message"]["content"]
    return custom_id, content


def extract_translations(text):
    result = []
    for line in text.split("\n"):
        if line.startswith("Нет перевода"):
            continue
        if line.startswith("нет перевода"):
            continue
        if not line.startswith("- "):
            logging.error("invalid format, bad prefix: %s", line)
            continue
        colon = line.find(": ", 3)
        if colon < 0:
            logging.error("invalid format, no colon: %s", line)
            continue
        w = line[2:colon]
        result.append(w)
    return result


def do_measure(word_set, inputs):
    scores = []
    for word_id, content in inputs:
        input_word = word_set[word_id]
        good = 0
        total = 0
        for w in extract_translations(content):
            total += 1
            if w not in input_word.translations:
                logging.info("unknown translation of %s: %s", input_word.word, w)
            else:
                good += 1
                # logging.info("known translation of %s: %s", input_word.word, w)
        logging.info("word %s: good %d out of %d", input_word.word, good, total)
        if total == 0:
            scores.append(0.0)
        else:
            scores.append(float(good) / total)
    logging.info("Score over %d points: %f", len(scores), sum(scores) / len(scores))


def measure_batch_output(args):
    word_set = load_word_set(args.input_words)
    assert len(word_set) >= 10

    with open(args.batch_id_path) as infile:
        batch_id = infile.read().strip()

    logging.info("Read batch ID: %s", batch_id)

    client = OpenAI()
    batch = client.batches.retrieve(batch_id)
    logging.info("Batch status: %s", batch.status)
    if batch.status != "completed":
        logging.error("Batch is not ready. Abort.")
        return 1
    batch_output_id = batch.output_file_id
    logging.info("Batch output file ID: %s", str(batch_output_id))
    file_response = client.files.content(batch_output_id)
    with open(args.batch_output, "wt") as outfile:
        outfile.write(file_response.text)

    inputs = []
    for jsonline in open(args.batch_output):
        obj = json.loads(jsonline)
        custom_id, content = extract_response(obj)
        assert content
        inputs.append((int(custom_id), content))

    do_measure(word_set, inputs)

    return 0


def validate_anthropic_model(model):
    assert model in ["claude-3-5-sonnet-20240620", "claude-3-haiku-20240307"]


def extract_anthropic(message):
    content = message.content[0]
    if content.type != "text":
        return []
    return content.text


def claude_translate(args):
    word_set = load_word_set(args.input_words)
    assert len(word_set) >= 10
    validate_anthropic_model(args.model)

    client = Anthropic()
    with open(args.batch_output, "wt") as outfile:
        for index, input_word in enumerate(word_set):
            message = client.messages.create(
                model=args.model,
                max_tokens=MAX_TOKENS,
                messages=[
                    {"role": "user", "content": build_prompt(input_word)}
                ]
            )
            content = extract_anthropic(message)
            logging.info("Content for word %s: %s", input_word.word, content)
            obj = {
                "word_id": index,
                "word": input_word.word,
                "content": content,
            }
            outfile.write(f"{json.dumps(obj)}\n")
    return 0


def claude_measure(args):
    word_set = load_word_set(args.input_words)
    assert len(word_set) >= 10

    inputs = []
    for jsonline in open(args.batch_output):
        obj = json.loads(jsonline)
        word_id = obj["word_id"]
        content = obj["content"]
        inputs.append((word_id, content))

    do_measure(word_set, inputs)

    return 0


def validate_google_model(model):
    assert model in ["gemini-1.5-pro", "gemini-1.5-flash"]


def gemini_translate(args):
    word_set = load_word_set(args.input_words)
    assert len(word_set) >= 10
    validate_google_model(args.model)

    genai.configure(api_key=os.environ["GOOGLE_AI_API_KEY"])

    model = genai.GenerativeModel(args.model)
    with open(args.batch_output, "wt") as outfile:
        for index, input_word in enumerate(word_set):
            response = model.generate_content(
                build_prompt(input_word),
                generation_config=genai.GenerationConfig(
                    max_output_tokens=MAX_TOKENS,
                )
            )
            try:
                content = response.text
            except Exception as e:
                logging.error("got exception for word %s: %s", input_word.word, str(e))
                content = "Нет перевода"
            logging.info("Content for word %s: %s", input_word.word, content)
            obj = {
                "word_id": index,
                "word": input_word.word,
                "content": content,
            }
            outfile.write(f"{json.dumps(obj)}\n")
    return 0


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers()

    launch_batch_parser = subparsers.add_parser("launch-batch")
    launch_batch_parser.add_argument("--input-words", required=True)
    launch_batch_parser.add_argument("--temp-batch", required=True)
    launch_batch_parser.add_argument("--model", required=True)
    launch_batch_parser.add_argument("--batch-id-path", required=True)
    launch_batch_parser.set_defaults(func=launch_batch)

    measure_batch_output_parser = subparsers.add_parser("measure-batch-output")
    measure_batch_output_parser.add_argument("--batch-id-path", required=True)
    measure_batch_output_parser.add_argument("--input-words", required=True)
    measure_batch_output_parser.add_argument("--batch-output", required=True)
    measure_batch_output_parser.set_defaults(func=measure_batch_output)

    claude_translate_parser = subparsers.add_parser("claude-translate")
    claude_translate_parser.add_argument("--input-words", required=True)
    claude_translate_parser.add_argument("--batch-output", required=True)
    claude_translate_parser.add_argument("--model", required=True)
    claude_translate_parser.set_defaults(func=claude_translate)

    claude_measure_parser = subparsers.add_parser("claude-measure")
    claude_measure_parser.add_argument("--input-words", required=True)
    claude_measure_parser.add_argument("--batch-output", required=True)
    claude_measure_parser.set_defaults(func=claude_measure)

    gemini_translate_parser = subparsers.add_parser("gemini-translate")
    gemini_translate_parser.add_argument("--input-words", required=True)
    gemini_translate_parser.add_argument("--batch-output", required=True)
    gemini_translate_parser.add_argument("--model", required=True)
    gemini_translate_parser.set_defaults(func=gemini_translate)

    args = parser.parse_args()
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
