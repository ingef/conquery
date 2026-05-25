# Version Extractor
FROM alpine/git:v2.52.0@sha256:3136372ed3c9e112d5a2620c66a6803e1b0b7f14a428fcbd0c5028bec4256430 AS version-extractor

WORKDIR /app
COPY .git .

RUN git describe --tags |  sed 's/^v//' > git_describe.txt

# Builder
FROM node:18-alpine@sha256:8d6421d663b4c28fd3ebc498332f249011d118945588d0a35cb9bc4b8ca09d9e AS builder

WORKDIR /app
COPY ./frontend/package.json ./frontend/package-lock.json ./

RUN npm ci

COPY ./frontend .

# Get the version from previous step
COPY --from=version-extractor /app/git_describe.txt .

# Uses env variables from .env file (BUILD TIME)
RUN PUBLIC_URL=/ npm run build

# The final image is just an nginx with a webroot
FROM nginxinc/nginx-unprivileged:1.29-alpine@sha256:ccbac1a4c20a8b41c5dd1691bd91d63eda3b7989d643a33fd47841838519bfb9

# This will be used by nginx's templating mechanism
# NGINX_PORT sets the container port on which nginx is listening
ENV NGINX_PORT=8000

# Copy the build artifacts from the builder phase
COPY --from=builder --chown=nginx /app/dist /usr/share/nginx/html/frontend
# Copy the env replacer
COPY --chown=nginx ./frontend/scripts/replace-env-at-runtime.sh /

# Copy nginx config template
COPY --chown=nginx ./frontend/container/ /

