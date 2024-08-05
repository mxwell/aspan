import argparse
import logging
import re

KNOWN_TAGS = {
    "a": "ADJ",
    "s": "ADJ",
    "r": "ADV",
    "n": "NOUN",
    "v": "VERB",
}


def parse_pos_tag(s):
    return KNOWN_TAGS.get(s)


def fix_word(s):
    bpos = s.find("(")
    if bpos >= 0:
        w = s[:bpos]
    else:
        w = s
    return w.replace("_", " ")


def parse_wordnet(dl_path, words_file):
    start_pattern = re.compile("^\d{8} ")
    word_pattern = re.compile("^[A-Za-z-'/ ]+$")
    seen = set()
    for rawline in open(dl_path):
        if not start_pattern.search(rawline):
            continue
        parts = rawline.strip().split(" ")
        if len(parts) < 4:
            continue
        pos_tag = parse_pos_tag(parts[2])
        if not pos_tag:
            logging.error("Unknown POS tag: %s", parts[2])
            continue
        word_num = int(parts[3], 16)
        for wid in range(0, word_num):
            word = fix_word(parts[4 + wid * 2])
            if not word_pattern.match(word):
                logging.info("Suspicious word: %s", word)
                continue
            entry = (pos_tag, word)
            if entry in seen:
                continue
            else:
                seen.add(entry)
            words_file.write(f"{pos_tag}: {word}\n")


def main():
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser()
    parser.add_argument("--dl-path", required=True, help="Path to downloaded Wordnet data.* file")
    parser.add_argument("--words-path", required=True, help="Path to output: POS tagged words")
    args = parser.parse_args()

    with open(args.words_path, "wt") as words_file:
        parse_wordnet(args.dl_path, words_file)


if __name__ == "__main__":
    main()
