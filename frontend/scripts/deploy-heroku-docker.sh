#!/bin/bash
APP_NAME=$1
cd ${TRAVIS_BUILD_DIR}/frontend/
echo "$HEROKU_TOKEN" | docker login -u _ registry.heroku.com --password-stdin
heroku -v
heroku container:push web -a $APP_NAME
heroku container:release web -a $APP_NAME
