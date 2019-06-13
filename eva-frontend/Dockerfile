FROM node:10.15-alpine

EXPOSE 8000

RUN npm set progress=false && \
    npm install -g yarn && \
    mkdir eva

COPY ./package.json ./yarn.lock /eva/
COPY ./conquery-v0.1.0.tgz /eva/
RUN cd /eva && \
    yarn

COPY . /eva
RUN cd /eva && yarn run build

CMD cd /eva && yarn run start-production && tail -f /dev/null
