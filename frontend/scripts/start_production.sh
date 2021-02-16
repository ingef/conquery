#!/bin/bash
set -e
set -x

# Execute from root folder

docker build -t conquery:v1 .
./scripts/stop_container.sh conquery
docker run -p 8000:8000 --name conquery conquery:v1
