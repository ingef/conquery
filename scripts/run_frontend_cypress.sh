#!/bin/bash
set -e


if [ ! -e ./frontend/build/index.html ]
then
    echo "Frontend build not found. Tying to install dependencies and build it."
    ./scripts/install_frontend.sh
    ./scripts/build_frontend.sh
fi

pushd "./frontend"

./scripts/replace-env-at-runtime.sh ./.env.e2e
yarn serve 

popd