#!/usr/bin/env bash

set -e

pushd ./frontend
npm ci
npm run build
popd
