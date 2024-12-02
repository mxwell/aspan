import logging


class Auth(object):

    def extract_user_id_from_token(self, token):
        return 1

    def verify_cron_token(self, body_json):
        token = body_json.get("CRON_TOKEN", "")
        logging.info("verify_cron_token: %s, no check", token)
        return True
