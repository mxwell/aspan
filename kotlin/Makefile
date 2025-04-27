api-server/build/distributions/api-server.zip: ./gradlew api-server/src
	./gradlew :api-server:assembleDist

api-server/build/distributions/api-server: api-server/build/distributions/api-server.zip
	cd api-server/build/distributions && unzip api-server.zip

build_runtime_image: Dockerfile api-server/build/distributions/api-server
	docker build --tag cr.yandex/crp33sksvqbe0tmf8sj2/kazakhverb/gymapi:v1 .
