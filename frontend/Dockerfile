FROM mhart/alpine-node:14
RUN apk add --no-cache bash

RUN mkdir /conquery
WORKDIR /conquery

COPY ./package.json ./yarn.lock ./
RUN yarn --no-progress --frozen-lockfile

COPY . .

# Uses env variables from .env file (BUILD TIME)
RUN PUBLIC_URL=/ yarn build

# To allow passing env variables at RUN TIME
# we're injecting them into the built artifacts.
# See `./scripts/replace-env-at-runtime.sh`, `.env`, `.env.example` for details
ENV REACT_APP_API_URL=$REACT_APP_API_URL
ENV REACT_APP_DISABLE_LOGIN=$REACT_APP_DISABLE_LOGIN
ENV REACT_APP_LANG=$REACT_APP_LANG
ENV REACT_APP_BASENAME=$REACT_APP_BASENAME
ENV REACT_APP_IDP_ENABLE=$REACT_APP_IDP_ENABLE
ENV REACT_APP_IDP_URL=$REACT_APP_IDP_URL
ENV REACT_APP_IDP_REALM=$REACT_APP_IDP_REALM
ENV REACT_APP_IDP_CLIENT_ID=$REACT_APP_IDP_CLIENT_ID

CMD /bin/bash ./scripts/replace-env-at-runtime.sh && PORT=8000 NODE_ENV=production node server

EXPOSE 8000
