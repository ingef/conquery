FROM node:8.9-alpine

RUN mkdir /conquery
WORKDIR /conquery

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress

COPY . .
RUN yarn run build-app

CMD yarn run start-production && tail -f webapp.log webapp-err.log

EXPOSE 8000
