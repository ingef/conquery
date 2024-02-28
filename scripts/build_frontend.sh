#!/bin/bash

pushd ./frontend
npm ci
npm run build
popd