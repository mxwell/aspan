IMAGE_TAG="un"

.PHONY: install_deps populate_db debug_server upload_files deploy_local bash_in_image stop_image upload_export

install_deps: requirements.txt
	pip3 install --user -r requirements.txt

populate_db:
	sqlite3 gc.db < testdata/words_translations.sql

debug_server:
	python3 -m flask --app app.py run

upload_files:
	scp -r app.py container_entry.sh lib Makefile .secrets g1:/gc-bundle/

deploy_local:
	scp -r app.py container_entry.sh lib Makefile .secrets /gc-bundle/

logs:
	mkdir -p $@

sockets:
	mkdir -p $@

bash_in_image: logs sockets .secrets
	docker run -it --rm --network host -v "${PWD}:/workdir" ${IMAGE_TAG} bash

run_image: logs sockets .secrets
	docker run -d --rm --name gc_server -v "${PWD}:/workdir" ${IMAGE_TAG}

stop_image:
	docker stop gc_server

exported_translations.jsonl: gc.db
	python3 scripts/export.py export-translations --db-path $< --jsonl $@

upload_export: exported_translations.jsonl
	python3 scripts/export.py upload-export --jsonl $< --db-path gc.db

build_runtime_image: app.py Dockerfile requirements.txt
	docker build --tag cr.yandex/crp33sksvqbe0tmf8sj2/kazakhverb/gc:v1 .
