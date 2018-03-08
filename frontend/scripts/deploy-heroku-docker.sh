#!/bin/bash
APP_NAME=$1
cd ${TRAVIS_BUILD_DIR}/frontend/
docker login -u _ -p "$HEROKU_TOKEN" registry.heroku.com
docker build -t registry.heroku.com/$APP_NAME/web .
docker push registry.heroku.com/$APP_NAME/web