import argparse
import logging
import os
import requests
import sys


ENDPOINT = "transcribe.api.cloud.yandex.net/speech/stt/v2/longRunningRecognize"
STATUS_ENDPOINT = "operation.api.cloud.yandex.net"


def build_body_json(audio_url):
    body = {
        "config": {
            "specification": {
                "languageCode": "kk-KZ",
                "model": "deferred-general",
                "profanityFilter": False,
                "literature_text": True,
                "audioEncoding": "MP3",
                "audioChannelCount": 2,
                "rawResults": True
            }
        },
        "audio": {
            "uri": audio_url
        }
    }
    return body


SAVED_IAM_TOKEN = None


def get_iam_token():
    global SAVED_IAM_TOKEN

    if SAVED_IAM_TOKEN is None:
        iam_token = os.environ.get("IAM_TOKEN", "")
        assert len(iam_token) > 0, "Don't forget about export IAM_TOKEN="
        logging.info("found IAM token with length %d", len(iam_token))
        SAVED_IAM_TOKEN = iam_token
    return SAVED_IAM_TOKEN


def build_headers():
    iam_token = get_iam_token()
    headers = {
        "Authorization": f"Bearer {iam_token}"
    }
    return headers


def asr_async(args):
    audio_name = args.audio_name
    assert audio_name.endswith(".mp3")

    audio_url = f"https://storage.yandexcloud.net/kazakhverb/{audio_name}"
    logging.info("audio url %s", audio_url)

    url = f"https://{ENDPOINT}"

    logging.info("url %s", url)
    body_json = build_body_json(audio_url)
    logging.info("body json: %s", body_json)

    headers = build_headers()

    response = requests.post(
        url,
        json=body_json,
        headers=headers
    )
    response.raise_for_status()
    print(response.json())


def check_status(args):
    operation_id = args.operation_id
    assert len(operation_id) > 0

    url = f"https://{STATUS_ENDPOINT}/operations/{operation_id}"
    headers = build_headers()

    response = requests.get(url, headers=headers)
    response.raise_for_status()
    print(response.json())


def main():
    LOG_FORMAT = "%(asctime)s %(threadName)s %(message)s"
    logging.basicConfig(format=LOG_FORMAT, level=logging.INFO)

    parser = argparse.ArgumentParser()

    subparsers = parser.add_subparsers()

    asr_async_parser = subparsers.add_parser("asr_async")
    asr_async_parser.add_argument("--audio-name", type=str, required=True)
    asr_async_parser.set_defaults(func=asr_async)

    check_status_parser = subparsers.add_parser("check_status")
    check_status_parser.add_argument("--operation-id", type=str, required=True)
    check_status_parser.set_defaults(func=check_status)

    args = parser.parse_args()
    args.func(args)
    return 0


if __name__ == '__main__':
    sys.exit(main())
