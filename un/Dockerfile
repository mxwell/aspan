FROM python:3.9.16-slim-bullseye

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

RUN apt update && \
    apt install -y ffmpeg gcc make && \
    rm -rf /var/lib/apt/lists/*

RUN pip install \
    PyJWT==2.3.0 \
    flask \
    google-auth \
    uwsgi \
    boto3 \
    yandex-speechkit==1.3.3

WORKDIR /workdir

CMD [ "bash", "/workdir/container_entry.sh" ]