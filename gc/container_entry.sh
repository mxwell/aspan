#!/usr/bin/env bash

set -xe

export APP_WORKDIR="/workdir"

# If you want to test with curl against the socket locally, without a proxy like nginx,
# add this argument: --protocol=http
#
# Then you can send requests like this:
# $ curl --unix-socket /path/to/gc/sockets/gc.socket "http://localhost/api/v1/test"
uwsgi \
    --socket "${APP_WORKDIR}/sockets/gc.socket" \
    --chmod-socket=666 \
    --wsgi-file "${APP_WORKDIR}/app.py" \
    --pyargv "" \
    --callable app \
    --logger "file:logfile=${APP_WORKDIR}/logs/gc.log,maxsize=2000000"
