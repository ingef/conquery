FROM node:8.9-alpine

RUN mkdir /conquery
WORKDIR /conquery

# Needed for git dependency of font-awesome-webpack
# Remove when font-awesome-webpack has upgraded its deps or we switch to something else
RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress --frozen-lockfile

COPY . .
RUN yarn run build-app

CMD NODE_ENV=production node app/server

EXPOSE 8000
