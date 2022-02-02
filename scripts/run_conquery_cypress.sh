#!/bin/env bash
proj_dir=$(readlink -f `dirname $0`/..)

"$proj_dir/scripts/run_backend_cypress.sh" &

pushd "$proj_dir/frontend"

export TSC_COMPILE_ON_ERROR=true
export REACT_APP_API_URL="http://localhost:8080"
export REACT_APP_DISABLE_LOGIN=true
export REACT_APP_LANG=de
export REACT_APP_IDP_ENABLE=false
export REACT_APP_IDP_REALM=Myrealm
export REACT_APP_IDP_CLIENT_ID=frontend

yarn start 
popd