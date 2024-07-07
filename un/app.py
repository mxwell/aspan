import argparse
import logging
from logging.config import dictConfig
import os
from os.path import join as pj
import sys

from flask import Flask, jsonify, request, make_response, send_file
from speechkit import model_repository, configure_credentials, creds

dictConfig({
    'version': 1,
    'formatters': {'default': {
        'format': '[%(asctime)s] %(levelname)s in %(module)s: %(message)s',
    }},
    'handlers': {'wsgi': {
        'class': 'logging.StreamHandler',
        'stream': 'ext://flask.logging.wsgi_errors_stream',
        'formatter': 'default'
    }},
    'root': {
        'level': 'INFO',
        'handlers': ['wsgi']
    }
})
DATABASE_PATH = "un.db"
app = Flask("un_app")
unInstance = None


def read_token(path):
    return open(path).read().strip()


class Un(object):

    def __init__(self, audio_workdir, yc_api_key):
        self.audio_workdir = audio_workdir
        self.counter = 101

        while True:
            path = self.make_filepath_for_counter(self.counter)
            if not os.path.exists(path):
                break
            self.counter += 1

        logging.info("Counter initialized to %d", self.counter)

        logging.info("YSK: configuring...")
        assert isinstance(yc_api_key, str), f"Invalid type: {type(yc_api_key)}"

        yc_api_key_text = read_token(yc_api_key)
        configure_credentials(
            yandex_credentials=creds.YandexCredentials(
                api_key=yc_api_key_text,
            )
        )
        logging.info("YSK: configured credentials")

        self.model = model_repository.synthesis_model()
        self.model.voice = "amira"

    def make_filepath_for_counter(self, counter):
        name = "gen_{:03d}".format(counter)
        path_prefix = pj(self.audio_workdir, name)
        return "{}.mp3".format(path_prefix)

    def make_filepath(self):
        cur = self.counter
        self.counter += 1
        return self.make_filepath_for_counter(cur)

    def generate(self, text):
        path = self.make_filepath()
        result = self.model.synthesize(text, raw_format=False)
        result.export(path, format="mp3")
        logging.info("Generated audio for %s: %s", text, path)
        return path


def init_un_app():
    global unInstance
    unInstance = Un("audio_workdir", ".secrets/.yc.apikey")
    logging.info("Un app initialized")


@app.route("/api/v1/test", methods=["GET"])
def get_test():
    return jsonify({"message": "You've reached Un!"}), 200


@app.route("/api/v1/tts", methods=["GET"])
def get_sound():
    global unInstance

    form = request.args.get("f")

    logging.info("Request: [%s]", form)

    if form:
        path = unInstance.generate(form)
        return send_file(path, as_attachment=False)
    else:
        return jsonify({"message": "Invalid request"}), 400


def main():
    raise NotImplementedError("This script is not meant to be run as a standalone application")


if __name__ == '__main__':
    sys.exit(main())
else:
    init_un_app()
