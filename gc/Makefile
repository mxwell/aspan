.PHONY: install_deps populate_db debug_server

install_deps: requirements.txt
	pip3 install --user -r requirements.txt

populate_db:
	sqlite3 gc.db < testdata/words_translations.sql

debug_server:
	python3 -m flask --app app.py run

upload_files:
	scp -r app.py container_entry.sh Dockerfile lib Makefile .secrets g1:/gc-bundle/