FROM node:8.9-alpine

EXPOSE 8000

RUN npm set progress=false && \
    mkdir conquery

COPY ./package.json ./yarn.lock /conquery/
RUN cd /conquery && \
    yarn

COPY . /conquery
RUN cd /conquery && yarn run build-app

CMD cd /conquery && yarn run start-production && tail -f /dev/null
