"""
Usage:

python3 scripts/extract_translations_from_wiktionary_dump.py --verbs-path data/verbs.txt --tei-path data/wiktionary_kk-ru_latest.tei > data/verbs_with_ru.wkt.csv

"""

import argparse
import json
import logging
import re
from lxml import etree


def load_verbs(path):
    verbs = set()
    counter = 0
    for line in open(path):
        verb = line.strip()
        verbs.add(verb)
        counter += 1
    logging.info("Read %s lines from %s: %d unique verb(s)", counter, path, len(verbs))
    return verbs


RU_VERB = re.compile(r"[а-я](ть|ться|ти|тись|чь|чься)\b")


def clean_translation(ctx, s):
    orig = s
    while True:
        braces_start = s.find("{{")
        if braces_start < 0:
            break

        braces_end = s.find("}}", braces_start)
        if braces_end <= braces_start:
            logging.warning("Dropping malformed translation [%s], context [%s]", orig, ctx)
            return None
        s = f"{s[:braces_start]}{s[braces_end + 2:]}"

    if s.find("}}") >= 0:
        logging.warning("Dropping malformed translation [%s], context [%s]", orig, ctx)
        return None

    if not RU_VERB.search(s):
        logging.warning("Dropping translation as non-verb: [%s], context [%s]", s, ctx)
        return None

    return s


def scan_tei(path, verbs_set):
    tree = etree.parse(path)

    namespaces = {'x': 'http://www.tei-c.org/ns/1.0'}

    wiktionary_entries = 0
    wiktionary_verbs = 0
    wiktionary_verbs_with_translation = 0
    wiktionary_verb_translations = 0
    for entry in tree.xpath('//x:entry', namespaces=namespaces):
        wiktionary_entries += 1
        orth = entry.xpath('x:form/x:orth', namespaces=namespaces)[0]
        word = orth.text

        if word not in verbs_set:
            continue
        wiktionary_verbs += 1

        quotes = entry.xpath('x:sense/x:cit[@type="trans"]/x:quote', namespaces=namespaces)

        parts = [word]
        translations = []

        for quote in quotes:
            cleaned = clean_translation(word, quote.text)
            if cleaned is None:
                continue
            parts.append(cleaned)
            translations.append(cleaned)

        if len(translations) == 0:
            continue
        wiktionary_verbs_with_translation += 1
        wiktionary_verb_translations += len(translations)

        print("\t".join(parts))

    avg_translations = 0.0 if wiktionary_verbs == 0 else float(wiktionary_verb_translations) / wiktionary_verbs
    logging.info(
        "TEI scan complete: %d dictionary entries, %d matched with known verbs, %d verbs have translation, %.2f translations on average",
        wiktionary_entries,
        wiktionary_verbs,
        wiktionary_verbs_with_translation,
        avg_translations
    )


def main():
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser()
    parser.add_argument("--verbs-path", required=True, help="Path to file with a list of verbs")
    parser.add_argument("--tei-path", required=True, help="Path to TEI file with a Wiktionary dump")
    args = parser.parse_args()

    verbs_set = load_verbs(args.verbs_path)
    scan_tei(args.tei_path, verbs_set)


if __name__ == "__main__":
    main()
