#! /bin/bash

set -xe

export COMPILED="BUILD/compiled.js"
export ASPAN_JS="BUILD/aspan.js"
export GENERATOR="scripts/bulk_generate.js"
export INPUT_VERBS="data/verbs.txt"
export OUTPUT_VERB_FORMS="data/verb_forms.csv"
export OUTPUT_SUGGEST_FORMS="data/suggest_forms.csv"

tsc --lib es2015 src/*.ts -t es6 -outFile $COMPILED

# add lines that export stuff
cat $COMPILED module_export.js > $ASPAN_JS

node $GENERATOR ${INPUT_VERBS} ${OUTPUT_VERB_FORMS}
node $GENERATOR --suggest-format ${INPUT_VERBS} ${OUTPUT_SUGGEST_FORMS}
echo "Done. Check ${OUTPUT_VERB_FORMS} and ${OUTPUT_SUGGEST_FORMS}."
