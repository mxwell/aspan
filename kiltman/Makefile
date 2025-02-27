.PHONY: build_kiltman upload_trie upload_files run_kiltman

build_kiltman:
	docker run --rm -it -v $(PWD):/kiltman ubuntu:18.04 bash

upload_trie: build/detect_suggest_trie.20240522.txt
	scp -r $< gnc:kiltman-bundle/

upload_files: build/kiltman
	scp -r $< build/lib nginx/ Makefile gnc:kiltman-bundle/

run_kiltman:
	export LD_LIBRARY_PATH=${PWD}/lib && ./kiltman load detect_suggest_trie.20240522.txt

build_runtime_image: static_build/kiltman Dockerfile
	docker build --tag cr.yandex/crp33sksvqbe0tmf8sj2/kazakhverb/kiltman:v1 .
