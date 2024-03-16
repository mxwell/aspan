.PHONY: tank_container tank_container_bash

tank_container:
	docker run -v $(PWD):/var/loadtest --net host -it --rm yandex/yandex-tank

tank_container_bash:
	docker run -v $(PWD):/var/loadtest --net host -it --rm --entrypoint /bin/bash yandex/yandex-tank