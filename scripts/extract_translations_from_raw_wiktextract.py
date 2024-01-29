"""
This handles files from https://kaikki.org/dictionary/rawdata.html, under "Download raw Wiktextract data (JSON, one object per line)"

Preliminary filtering:

  fgrep '"lang_code": "kk"' <INPUT_FILE> | fgrep '"pos": "verb"'

Preview is possible with:

  jq -r '.word as $word | .senses[].glosses[] | "\($word): \(.)"' <INPUT FILE>

Usage:

python3 scripts/extract_translations_from_raw_wiktextract.py --verbs-path data/verbs.txt --wiktextract-path data/raw-wiktextract-data.kk.verb.json > data/verbs_with_en.wkt.csv
"""

import argparse
import json
import logging


def load_verbs(path):
    verbs = set()
    counter = 0
    for line in open(path):
        verb = line.strip()
        verbs.add(verb)
        counter += 1
    logging.info("Read %s lines from %s: %d unique verb(s)", counter, path, len(verbs))
    return verbs


def clean_translation(ctx, s):
    if not s.startswith("to "):
        logging.warning("Dropping translation as non-verb: [%s], context [%s]", s, ctx)
        return None
    return s


def scan_wiktextract(path, verbs_set):
    wiktionary_entries = 0
    wiktionary_verbs = 0
    wiktionary_verb_translations = 0
    verbs_with_translations = set()
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
        parts = [word]
        translations = []
        for sense in jo["senses"]:
            if "glosses" not in sense:
                continue
            for gloss in sense["glosses"]:
                translation = clean_translation(word, gloss)
                if translation is None:
                    continue
                parts.append(gloss)
                translations.append(gloss)

        if len(translations) == 0:
            continue
        wiktionary_verb_translations += len(translations)
        verbs_with_translations.add(word)

        print("\t".join(parts))

    avg_translations = 0.0 if wiktionary_verbs == 0 else float(wiktionary_verb_translations) / wiktionary_verbs
    logging.info(
        "wiktextract scan complete: %d dictionary entries, %d matched with known verbs, %d verbs have translation, %.2f translations on average",
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
    parser.add_argument("--wiktextract-path", required=True, help="Path to raw-wiktextract-data.json file with a Wiktionary dump")
    args = parser.parse_args()

    verbs_set = load_verbs(args.verbs_path)
    verbs_with_translations = scan_wiktextract(args.wiktextract_path, verbs_set)
    print_verbs_without_translations(verbs_set, verbs_with_translations)


if __name__ == "__main__":
    main()
