#!/bin/bash

pushd ./frontend
yarn --ignore-platform --no-progress --frozen-lockfile
yarn build
popd