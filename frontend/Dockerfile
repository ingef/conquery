FROM mhart/alpine-node:14

RUN mkdir /conquery
WORKDIR /conquery

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress --frozen-lockfile

COPY . .
RUN PUBLIC_URL=/ yarn build

CMD PORT=8000 NODE_ENV=production node server

EXPOSE 8000
