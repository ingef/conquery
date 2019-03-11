FROM node:8.12-alpine

RUN mkdir /conquery
WORKDIR /conquery

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress --frozen-lockfile

COPY . .
RUN yarn run build

CMD NODE_ENV=production node app/server

EXPOSE 8000
