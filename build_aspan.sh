#! /bin/bash

set -xe

export COMPILED="BUILD/compiled.js"
export OUTPUT_PATH="train/static/src/javascripts/lib/aspan.js"

tsc --lib es2015 src/*.ts -t es6 -outFile $COMPILED

# add "tail" with lines that export stuff
cat $COMPILED tail.js > $OUTPUT_PATH

echo "Output is saved to ${OUTPUT_PATH}"
