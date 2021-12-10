FROM node:14.7.0-buster-slim

RUN npm install -g typescript

WORKDIR /frontend