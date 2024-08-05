import argparse
import logging
from lxml import etree


KNOWN_TAGS = {
    "ADJF": "ADJ",
    "ADVB": "ADV",
    "CONJ": "SCONJ",  # CONJ also includes CCONJ, so needs post-processing
    "INTJ": "INTJ",
    "NOUN": "NOUN",
    "NUMR": "NUM",
    "PREP": "ADP",
    "NPRO": "PRON",
    "INFN": "VERB",
    "PRCL": "PART",
}


def parse_pos_tag(s):
    return KNOWN_TAGS.get(s)


def parse_xml(dl_path, words_file):
    tree = etree.parse(dl_path)
    root = tree.getroot()

    g_to_skip = {
        "Abbr",
        "Geox",
        "Name",
        "Orgn",  # organization
        "Patr",  # patronymic
        "Slng",  # slang
        "Surn",  # surname
        "Trad",
    }
    forced_cconj = {
        "а",
        "да",
        "и",
        "или",
        "но",
        "однако",
        "либо",
    }

    # Iterate over lemmas
    for lemma in root.xpath('//lemma'):
        # Get the main form (first 'l' element)
        main_form = lemma.xpath('./l/@t')[0]

        # Get the part-of-speech tag (first 'g' element within 'l')
        pos_tag_raw = lemma.xpath('./l/g/@v')[0]

        pos_tag = parse_pos_tag(pos_tag_raw)
        if pos_tag is None:
            continue

        skip = False
        for g_entry in lemma.xpath("./l/g/@v"):
            if g_entry in g_to_skip:
                skip = True
                break
        if skip:
            continue

        if pos_tag == "ADJ":
            for g_entry in lemma.xpath("./l/g/@v"):
                if g_entry == "Anum":  # ordinal number
                    pos_tag = "NUM"
                    logging.info("Replaced POS from ADJ to NUM: %s", main_form)
                    break
                if g_entry == "Apro":  # pronoun
                    pos_tag = "PRON"
                    logging.info("Replaced POS from ADJ to PRON: %s", main_form)
                    break
                if g_entry == "Poss":
                    skip = True
                    break
        elif pos_tag == "SCONJ":
            if main_form in forced_cconj:
                pos_tag = "CCONJ"

        if skip:
            continue

        # print(f"Main form: {main_form}, POS: {pos_tag}")
        words_file.write(f"{pos_tag}: {main_form}\n")


def main():
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser()
    parser.add_argument("--dl-path", required=True, help="Path to downloaded OpenCorpora data (XML)")
    parser.add_argument("--words-path", required=True, help="Path to output: POS tagged words")
    args = parser.parse_args()

    with open(args.words_path, "wt") as words_file:
        parse_xml(args.dl_path, words_file)


if __name__ == "__main__":
    main()
