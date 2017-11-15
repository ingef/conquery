#
# This Dockerfile creates an image that allows to deploy the app
#

FROM node:6.5.0

EXPOSE 8000

RUN npm set progress=false && \
    npm install -g yarn && \
    mkdir conquery

COPY ./scripts /conquery/scripts
COPY ./package.json ./yarn.lock /conquery/
RUN cd /conquery && \
    yarn

COPY . /conquery
RUN cd /conquery && yarn run build

CMD cd /conquery && yarn run start-production && tail -f /dev/null
