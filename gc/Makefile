.PHONY: debug_server

populate_db:
	sqlite3 gc.db < testdata/words_translations.sql

debug_server:
	python3 -m flask --app app.py run
