FROM python:3.9.16-slim-bullseye

WORKDIR /workdir

COPY ./requirements.txt /workdir/requirements.txt

RUN pip install --no-cache-dir --upgrade -r /workdir/requirements.txt

COPY ./lib /workdir/lib

CMD ["gunicorn", "--conf", "lib/gunicorn_conf.py", "--bind", "0.0.0.0:80", "lib.gunicorn_entry:app"]
