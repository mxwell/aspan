"""
Usage:

python3 scripts/assign_form_weights.py \
  --frequencies data/freq/sozforma.txt \
  --forms kiltman/build/combined.20241119.jsonl \
  --weighted-forms kiltman/build/combined.weighted.20241119.jsonl

"""

import argparse
import json
import logging


def load_form_weights(frequencies_path):
    result = dict()
    max_freq = None
    for rawline in open(frequencies_path):
        parts = rawline.strip().split("\t")
        assert len(parts) == 2
        freq = int(parts[0])
        if max_freq is None:
            max_freq = float(freq)
        freq_float = freq / max_freq
        form = parts[1].lower()
        result[form] = freq_float
    return result


def assign_form_weights(form_weights, input_path, output_path):
    with open(output_path, "wt") as output_file:
        form_counter = 0
        for rawline in open(input_path):
            obj = json.loads(rawline)
            weighted_forms = []
            for form in obj["forms"]:
                weight = form_weights.get(form["form"], 0.0)
                form["weight"] = weight
                weighted_forms.append(form)
                form_counter += 1
            obj["forms"] = weighted_forms
            output_line = json.dumps(obj)
            output_file.write(f"{output_line}\n")
        logging.info("Assigned weights to %d forms", form_counter)


def main():
    logging.basicConfig(level=logging.INFO)
    parser = argparse.ArgumentParser(description="Assign weights to combined forms using word form frequencies")
    parser.add_argument("--frequencies", help="Path to TSV file with frequencies and word forms")
    parser.add_argument("--forms", help="Path to JSON linse file with forms, it's input")
    parser.add_argument("--weighted-forms", help="Path to JSON linse file with forms with assigned weights, it's output")
    args = parser.parse_args()

    form_weights = load_form_weights(args.frequencies)
    logging.info("Loaded %d forms with weights", len(form_weights))

    assign_form_weights(form_weights, args.forms, args.weighted_forms)


main()