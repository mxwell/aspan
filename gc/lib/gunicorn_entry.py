import logging

from .gcapp import app, init_gc_app

init_gc_app()

#from flask import Flask
#app = Flask("gunicorn_test")

#@app.route("/")
#def hello():
#    return "<h1 style='color:blue'>Hello There!</h1>"
