#!/bin/bash

pushd ./frontend
yarn --ignore-platform --frozen-lockfile
yarn run react-app-rewired build
popd