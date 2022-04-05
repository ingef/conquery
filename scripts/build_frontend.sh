#!/bin/bash

pushd ./frontend
TSC_COMPILE_ON_ERROR=true yarn run react-app-rewired build
popd