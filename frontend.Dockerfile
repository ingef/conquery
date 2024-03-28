# Version Extractor
FROM bitnami/git:2.38.1 AS version-extractor

WORKDIR /app
COPY .git .

RUN git describe --tags |  sed 's/^v//' > git_describe.txt

# Builder
FROM node:18-alpine AS builder

WORKDIR /app
COPY ./frontend/package.json ./frontend/package-lock.json ./

RUN npm ci

COPY ./frontend .

# Get the version from previous step
COPY --from=version-extractor /app/git_describe.txt .

# Uses env variables from .env file (BUILD TIME)
RUN PUBLIC_URL=/ npm run build

# The final image is just an nginx with a webroot
FROM nginx:stable-alpine

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

# Copy the build artifacts from the builder phase
COPY --from=builder /app/dist /usr/share/nginx/html
# Copy the env replacer
COPY ./frontend/scripts/replace-env-at-runtime.sh /

# The default command replaces the environment variables and starts nginx as a subprocess
CMD [ "/bin/sh", "-c", "/replace-env-at-runtime.sh /usr/share/nginx/html/index.html && nginx -g \"daemon off;\""]


EXPOSE 80

