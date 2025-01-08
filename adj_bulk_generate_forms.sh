#! /bin/bash

set -xe

export COMPILED="BUILD/compiled.js"
export ASPAN_JS="BUILD/aspan.js"
export GENERATOR="scripts/adj_bulk_generate.js"

export INPUT_ADJS="data/dict/adj_kk.gc.v2.YYYYMMDD.csv"

export OUTPUT_DETECT_SUGGEST_FORMS="data/adj.detect_suggest_forms.jsonl"

tsc --lib es2015 src/*.ts -t es6 -outFile $COMPILED

# add lines that export stuff
cat $COMPILED module_export.js > $ASPAN_JS

node $GENERATOR detect_suggest_forms ${INPUT_ADJS} ${OUTPUT_DETECT_SUGGEST_FORMS}
echo "Generated detector+suggest forms data is stored to ${OUTPUT_DETECT_SUGGEST_FORMS}."
