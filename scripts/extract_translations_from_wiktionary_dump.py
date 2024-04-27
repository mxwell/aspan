"""
Usage:

python3 scripts/extract_translations_from_wiktionary_dump.py \
    --verbs-path data/verbs.txt \
    --tei-path data/wiktionary_kk-ru_latest.tei --tei-prefix ruwkt: \
    --wiktextract-path data/raw-wiktextract-data.kk.verb.json --wiktextract-prefix enwkt: \
    --verb-frequencies-path train/present_top100.colonsv \
    > data/verbs_with_ru_en.wkt.csv

How to prepare wiktextract:

  Wiktextract files are found on https://kaikki.org/dictionary/rawdata.html, under "Download raw Wiktextract data (JSON, one object per line)"

  Preliminary filtering:

    fgrep '"lang_code": "kk"' <INPUT_FILE> | fgrep '"pos": "verb"'

  Preview is possible with:

    jq -r '.word as $word | .senses[].glosses[] | "\($word): \(.)"' <INPUT FILE>

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


def clean_ru_gloss(ctx, s):
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


def load_from_tei(path, gloss_prefix, verbs_set):
    tree = etree.parse(path)

    namespaces = {'x': 'http://www.tei-c.org/ns/1.0'}

    wiktionary_entries = 0
    wiktionary_verbs = 0
    wiktionary_verb_translations = 0
    verbs_with_translations = set()
    result = dict()
    for entry in tree.xpath('//x:entry', namespaces=namespaces):
        wiktionary_entries += 1
        orth = entry.xpath('x:form/x:orth', namespaces=namespaces)[0]
        word = orth.text

        if word not in verbs_set:
            continue
        wiktionary_verbs += 1

        quotes = entry.xpath('x:sense/x:cit[@type="trans"]/x:quote', namespaces=namespaces)

        translations = []

        for quote in quotes:
            cleaned = clean_ru_gloss(word, quote.text)
            if cleaned is None:
                continue
            translations.append(f"{gloss_prefix}{cleaned}")

        if len(translations) == 0:
            continue
        wiktionary_verb_translations += len(translations)
        verbs_with_translations.add(word)

        result[word] = translations

    avg_translations = 0.0 if wiktionary_verbs == 0 else float(wiktionary_verb_translations) / wiktionary_verbs
    logging.info(
        "TEI scan complete: %d dictionary entries, %d matched with known verbs, %d verbs have translation, %.2f translations on average",
        wiktionary_entries,
        wiktionary_verbs,
        len(verbs_with_translations),
        avg_translations
    )
    return result


def clean_en_gloss(ctx, s):
    if not s.startswith("to "):
        logging.warning("Dropping translation as non-verb: [%s], context [%s]", s, ctx)
        return None
    return s


def load_from_wiktextract(path, gloss_prefix, verbs_set):
    wiktionary_entries = 0
    wiktionary_verbs = 0
    wiktionary_verb_translations = 0
    verbs_with_translations = set()
    result = dict()
    for line in open(path):
        wiktionary_entries += 1
        jo = json.loads(line)
        if "word" not in jo:
            continue

        word = jo["word"]
        if word not in verbs_set:
            continue
        wiktionary_verbs += 1

        if "senses" not in jo:
            continue
        translations = []
        for sense in jo["senses"]:
            if "glosses" not in sense:
                continue
            for gloss in sense["glosses"]:
                translation = clean_en_gloss(word, gloss)
                if translation is None:
                    continue
                translations.append(f"{gloss_prefix}{translation}")

        if len(translations) == 0:
            continue
        wiktionary_verb_translations += len(translations)
        verbs_with_translations.add(word)

        result[word] = translations

    avg_translations = 0.0 if wiktionary_verbs == 0 else float(wiktionary_verb_translations) / wiktionary_verbs
    logging.info(
        "wiktextract scan complete: %d dictionary entries, %d matched with known verbs, %d verbs have translation, %.2f translations on average",
        wiktionary_entries,
        wiktionary_verbs,
        len(verbs_with_translations),
        avg_translations
    )
    return result


def load_verb_frequencies(path):
    result = dict()
    if path == "":
        return result

    for line in open(path):
        parts = line.strip().split(":")
        verb = parts[0]
        freq = int(parts[4])
        result[verb] = freq
    return result


def print_verbs(verbs_set, tei, wiktextract, verb_freq):
    ordered = sorted(list(verbs_set))
    tei_count = 0
    wiktextract_count = 0
    both_count = 0
    no_count = 0
    for verb in ordered:
        parts = [verb, str(verb_freq.get(verb, 0))]
        has_tei = False
        has_wiktextract = False
        if verb in tei:
            has_tei = True
            parts.extend(tei[verb])
        if verb in wiktextract:
            has_wiktextract = True
            parts.extend(wiktextract[verb])
        if has_tei:
            tei_count += 1
            if has_wiktextract:
                wiktextract_count += 1
                both_count += 1
        elif has_wiktextract:
            wiktextract_count += 1
        else:
            no_count += 1
        print("\t".join(parts))
    logging.info(
        "Printed %d verbs: %d with tei glosses, %d with wiktextract glosses, %d with both, %d without any glosses",
        len(ordered),
        tei_count,
        wiktextract_count,
        both_count,
        no_count,
    )


def main():
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser()
    parser.add_argument("--verbs-path", required=True, help="Path to file with a list of verbs")
    parser.add_argument("--tei-path", required=True, help="Path to TEI file with a Wiktionary dump")
    parser.add_argument("--tei-prefix", default="", help="Prefix for glosses from TEI, e.g. 'ruwkt:' in the output CSV")
    parser.add_argument("--wiktextract-path", required=True, help="Path to raw-wiktextract-data.json file with a Wiktionary dump")
    parser.add_argument("--wiktextract-prefix", default="", help="Prefix for glosses from wiktextract, e.g. 'enwkt:' in the output CSV")
    parser.add_argument("--verb-frequencies-path", default="", help="Path to file with verb frequencies")
    args = parser.parse_args()

    verbs_set = load_verbs(args.verbs_path)
    verbs_from_tei = load_from_tei(args.tei_path, args.tei_prefix, verbs_set)
    verbs_from_wiktextract = load_from_wiktextract(args.wiktextract_path, args.wiktextract_prefix, verbs_set)
    verb_frequencies = load_verb_frequencies(args.verb_frequencies_path)
    print_verbs(verbs_set, verbs_from_tei, verbs_from_wiktextract, verb_frequencies)


if __name__ == "__main__":
    main()
