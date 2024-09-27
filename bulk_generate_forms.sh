#! /bin/bash

set -xe

export COMPILED="BUILD/compiled.js"
export ASPAN_JS="BUILD/aspan.js"
export GENERATOR="scripts/bulk_generate.js"

export INPUT_VERBS="data/verbs_with_ru_en_v2.csv"

export OUTPUT_DETECTOR_FORMS="data/detector_forms.csv"
export OUTPUT_DETECT_SUGGEST_FORMS="data/detect_suggest_forms.jsonl"
export OUTPUT_SUGGEST_INFINITIVE_TRANSLATION="data/suggest_infinitive_translation.csv"
export OUTPUT_VERBS_WITH_META="data/verbs_fe_soft.csv"

tsc --lib es2015 src/*.ts -t es6 -outFile $COMPILED

# add lines that export stuff
cat $COMPILED module_export.js > $ASPAN_JS

# node $GENERATOR detector_forms ${INPUT_VERBS} ${OUTPUT_DETECTOR_FORMS}
# echo "Generated detector forms data is stored to ${OUTPUT_DETECTOR_FORMS}."

# node $GENERATOR detect_suggest_forms ${INPUT_VERBS} ${OUTPUT_DETECT_SUGGEST_FORMS}
# echo "Generated detector+suggest forms data is stored to ${OUTPUT_DETECT_SUGGEST_FORMS}."

# node $GENERATOR suggest_infinitive_translation ${INPUT_VERBS} ${OUTPUT_SUGGEST_INFINITIVE_TRANSLATION}
# echo "Generated infinitive and translation suggest data is stored to ${OUTPUT_SUGGEST_INFINITIVE_TRANSLATION}."

# export OUTPUT_PRESENT_CONTINUOUS_FORMS="data/present_continuous_forms.csv"
# node $GENERATOR present_continuous_forms ${INPUT_VERBS} ${OUTPUT_PRESENT_CONTINUOUS_FORMS}
# echo "Generated present continuous forms data is stored to ${OUTPUT_PRESENT_CONTINUOUS_FORMS}."

node $GENERATOR verbs_with_meta ${INPUT_VERBS} ${OUTPUT_VERBS_WITH_META}
echo "Generated verbs with meta data is stored to ${OUTPUT_VERBS_WITH_META}."