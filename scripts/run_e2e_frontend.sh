#!/bin/bash
set -e


echo
echo "Checking for frontend build"
if [ ! -e ./frontend/build/index.html ]
then
    echo "Frontend build not found. Tying to install dependencies and build it."
    ./scripts/install_frontend.sh
    ./scripts/build_frontend.sh
else
    echo "Frontend build found: ./frontend/build/index.html"
fi

pushd "./frontend"
echo "Using frontend env variables from .env.e2e"
(
	set -a
	source .env.e2e
	set +a
	./scripts/replace-env-at-runtime.sh ./build/index.html 
)

echo
echo "Starting frontend server"
yarn serve

popd
