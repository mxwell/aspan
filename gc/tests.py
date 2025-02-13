from lib import app, init_gc_app

from flask_testing import TestCase
import unittest


class GcAppTestCase(TestCase):
    def create_app(self):
        app.config['TESTING'] = True
        init_gc_app()
        return app

    def test_get_test_path(self):
        response = self.client.get("/gcapi/v1/test")
        self.assert200(response)
        self.assertEqual(response.json, {
            "message": "You've reached GC!",
        })

    def test_get_book_chunks(self):
        response = self.client.get("/gcapi/v1/get_book_chunks?book_id=1001")
        self.assert200(response)
        self.assertEqual(len(response.json["chunks"]), 1)

    def test_get_book_chunks10(self):
        response = self.client.get("/gcapi/v1/get_book_chunks?book_id=1001&count=10")
        self.assert200(response)
        self.assertEqual(len(response.json["chunks"]), 10)


if __name__ == '__main__':
    unittest.main()
