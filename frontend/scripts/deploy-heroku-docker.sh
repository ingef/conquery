#!/bin/bash
APP_NAME=$1
cd ${TRAVIS_BUILD_DIR}/frontend/
/usr/local/heroku/bin/heroku container:push web -a $APP_NAME
/usr/local/heroku/bin/heroku container:release web -a $APP_NAME
