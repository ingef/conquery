#!/usr/bin/env bash

set -e

pushd ./frontend
pnpm install --frozen-lockfile
pnpm build
popd
