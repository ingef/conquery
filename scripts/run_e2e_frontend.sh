#!/bin/bash
set -e


echo
echo "Checking for frontend build"
if [ ! -e ./frontend/build/index.html ]
then
    echo "Frontend build not found. Tying to install dependencies and build it."
    ./scripts/install_frontend.sh
    ./scripts/build_frontend.sh
    
    echo "Using frontend env variables from .env.e2e"
    ./scripts/replace-env-at-runtime.sh ./.env.e2e
else
    echo "Frontend build found: ./frontend/build/index.html"
fi

pushd "./frontend"


echo
echo "Starting frontend server"
yarn serve

popd