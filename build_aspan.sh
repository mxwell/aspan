#! /bin/bash

set -xe

export OUTPUT_PATH="train/static/src/javascripts/lib/aspan.js"

tsc types.ts str_manip.ts rules.ts phonetics.ts question.ts verb.ts -t es6 -outFile BUILD/output.js

# add "tail" with lines that export stuff
cat BUILD/output.js tail.js > $OUTPUT_PATH

echo "Output is saved to ${OUTPUT_PATH}"
