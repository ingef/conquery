#!/bin/bash
APP_NAME=$1
cd ${TRAVIS_BUILD_DIR}/frontend/
docker login -u _ -p "$HEROKU_TOKEN" registry.heroku.com
heroku -v
heroku container:push web -a $APP_NAME
heroku container:release web -a $APP_NAME
