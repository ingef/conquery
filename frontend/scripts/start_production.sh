#!/bin/bash
set -e
set -x

# Execute from root folder

export $(grep -v '^#' .env | xargs)

docker build -t conquery:v1 .
./scripts/stop_container.sh conquery
docker run -p $PORT:80 --name conquery conquery:v1
