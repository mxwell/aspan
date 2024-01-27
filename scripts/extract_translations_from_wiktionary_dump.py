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
                    close_counter = 0
            else:
                open_counter = 0
                close_counter = 0
            i += 1

        if braces_end <= braces_start:
            logging.warning("Dropping malformed translation [%s], context [%s]", orig, ctx)
            return None
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

    if not RU_VERB.search(s):
        logging.warning("Dropping translation as non-verb: [%s], context [%s]", s, ctx)
        return None

    return s


def scan_tei(path, verbs_set):
    tree = etree.parse(path)

    namespaces = {'x': 'http://www.tei-c.org/ns/1.0'}

    wiktionary_entries = 0
    wiktionary_verbs = 0
    wiktionary_verb_translations = 0
    verbs_with_translations = set()
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
        wiktionary_verb_translations += len(translations)
        verbs_with_translations.add(word)

        print("\t".join(parts))

    avg_translations = 0.0 if wiktionary_verbs == 0 else float(wiktionary_verb_translations) / wiktionary_verbs
    logging.info(
        "TEI scan complete: %d dictionary entries, %d matched with known verbs, %d verbs have translation, %.2f translations on average",
        wiktionary_entries,
        wiktionary_verbs,
        len(verbs_with_translations),
        avg_translations
    )
    return verbs_with_translations


def print_verbs_without_translations(verbs_set, verbs_with_translations):
    verbs = sorted(list(verbs_set.difference(verbs_with_translations)))
    for verb in verbs:
        print(verb)
    logging.info("Printed %d verbs without translation.", len(verbs))


def main():
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser()
    parser.add_argument("--verbs-path", required=True, help="Path to file with a list of verbs")
    parser.add_argument("--tei-path", required=True, help="Path to TEI file with a Wiktionary dump")
    args = parser.parse_args()

    verbs_set = load_verbs(args.verbs_path)
    verbs_with_translations = scan_tei(args.tei_path, verbs_set)
    print_verbs_without_translations(verbs_set, verbs_with_translations)


if __name__ == "__main__":
    main()
