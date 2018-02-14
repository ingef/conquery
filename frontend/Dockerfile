FROM node:8.9-alpine

RUN mkdir /conquery
WORKDIR /conquery

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress

COPY . .
RUN yarn run build-app

CMD NODE_ENV=production node app/server

EXPOSE 8000
