# Load testing

Tool: Yandex.Tank
Its docs: https://yandextank.readthedocs.io/en/latest/intro.html

## Steps

- get Docker image: `docker pull yandex/yandex-tank`
- replace HOSTNAME in two places within `load.yaml` with your actual hostname
- run Docker container: `make tank_container`

You will see a nice overview of the process in your terminal during the load testing.

After the finish, you can open `logs/<TIMESTAMP>/phout_<ID>.log` and look at or visualize its columns.
As of 2024, the most interesting columns:
- the first is Unix epoch time
- the second is request time
- the last is HTTP status code