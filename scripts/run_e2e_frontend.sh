#!/bin/bash
set -e


echo
echo "Checking for frontend build"
if [ ! -e ./frontend/dist/index.html ]
then
    echo "Frontend build not found. Tying to build it."
    ./scripts/build_frontend.sh
else
    echo "Frontend build found: ./frontend/dist/index.html"
fi

pushd "./frontend"
echo "Using frontend env variables from .env.e2e"
(
	set -a
	source .env.e2e
	set +a
	./scripts/replace-env-at-runtime.sh ./dist/index.html
)

echo
echo "Starting frontend server"
npm run serve

popd
