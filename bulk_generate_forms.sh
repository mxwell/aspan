#! /bin/bash

set -xe

export COMPILED="BUILD/compiled.js"
export ASPAN_JS="BUILD/aspan.js"
export GENERATOR="scripts/bulk_generate.js"

export INPUT_VERBS="data/verbs_with_ru_en.wkt.csv"

export OUTPUT_DETECTOR_FORMS="data/detector_forms.csv"
export OUTPUT_SUGGEST_INFINITIVE_TRANSLATION="data/suggest_infinitive_translation.csv"

tsc --lib es2015 src/*.ts -t es6 -outFile $COMPILED

# add lines that export stuff
cat $COMPILED module_export.js > $ASPAN_JS

node $GENERATOR detector_forms ${INPUT_VERBS} ${OUTPUT_DETECTOR_FORMS}
echo "Generated detector forms data is stored to ${OUTPUT_DETECTOR_FORMS}."

# node $GENERATOR suggest_infinitive_translation ${INPUT_VERBS} ${OUTPUT_SUGGEST_INFINITIVE_TRANSLATION}
# echo "Generated infinitive and translation suggest data is stored to ${OUTPUT_SUGGEST_INFINITIVE_TRANSLATION}."

# export OUTPUT_PRESENT_CONTINUOUS_FORMS="data/present_continuous_forms.csv"
# node $GENERATOR present_continuous_forms ${INPUT_VERBS} ${OUTPUT_PRESENT_CONTINUOUS_FORMS}
# echo "Generated present continuous forms data is stored to ${OUTPUT_PRESENT_CONTINUOUS_FORMS}."