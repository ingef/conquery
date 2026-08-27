# Version Extractor
FROM alpine/git:v2.52.0@sha256:3136372ed3c9e112d5a2620c66a6803e1b0b7f14a428fcbd0c5028bec4256430 AS version-extractor

WORKDIR /app
COPY .git .

RUN git describe --tags |  sed 's/^v//' > git_describe.txt

# Builder
FROM node:24-alpine@sha256:d32cdf619f63fe0471182d08996dd516c6275bb5fd31ae06e55a570bd9e1ad43 AS builder

WORKDIR /app
RUN npm install -g pnpm@11.24.0

COPY ./frontend/package.json ./frontend/pnpm-lock.yaml ./frontend/pnpm-workspace.yaml ./

RUN pnpm install --frozen-lockfile

COPY ./frontend .

# Get the version from previous step
COPY --from=version-extractor /app/git_describe.txt .

# Uses env variables from .env file (BUILD TIME)
RUN PUBLIC_URL=/ pnpm build

# The final image is just an nginx with a webroot
FROM nginxinc/nginx-unprivileged:1.31-alpine@sha256:8122337ed6c475bb486bc9340da453d4599f225e6b920ff0d92ca2267486b9b5

# This will be used by nginx's templating mechanism
# NGINX_PORT sets the container port on which nginx is listening
ENV NGINX_PORT=8000

# Copy the build artifacts from the builder phase
COPY --from=builder --chown=nginx /app/dist /usr/share/nginx/html/frontend
# Copy the env replacer
COPY --chown=nginx ./frontend/scripts/replace-env-at-runtime.sh /

# Copy nginx config template
COPY --chown=nginx ./frontend/container/ /

