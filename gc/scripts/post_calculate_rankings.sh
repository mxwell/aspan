#! /usr/bin/bash

set -xe

if [[ -z "$CRON_TOKEN" ]] ; then
    echo "Error: variable CRON_TOKEN is unset"
    exit 1
fi

GC_ENDPOINT=${GC_ENDPOINT:-http://127.0.0.1:5000}

curl --verbose \
    -X POST --location "${GC_ENDPOINT}/gcapi/v1/calculate_rankings" \
    -H "Content-Type: application/json" \
    -d "{\"CRON_TOKEN\": \"${CRON_TOKEN}\"}"
