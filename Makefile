IMAGE_TAG="typescript_compiler"

.PHONY: build_image builder

build_image: Dockerfile
	docker build --tag ${IMAGE_TAG} --file $< .

builder:
	docker run -it --rm -v "${PWD}:/frontend" ${IMAGE_TAG} bash
