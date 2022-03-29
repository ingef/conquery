#!/bin/bash

# check first argument, if it is an existing file
if [ -r $1 ]
then
    # if so, we asume it is an env-file and we source it
    . $1
fi

ENVSTRING=""
ENVSTRING+="REACT_APP_API_URL: \"${REACT_APP_API_URL:-null}\","
ENVSTRING+="REACT_APP_DISABLE_LOGIN: \"${REACT_APP_DISABLE_LOGIN:-null}\"," 
ENVSTRING+="REACT_APP_LANG: \"${REACT_APP_LANG:-null}\","
ENVSTRING+="REACT_APP_BASENAME: \"${REACT_APP_BASENAME:-null}\","
ENVSTRING+="REACT_APP_IDP_ENABLE: \"${REACT_APP_IDP_ENABLE:-null}\","
ENVSTRING+="REACT_APP_IDP_URL: \"${REACT_APP_IDP_URL:-null}\","
ENVSTRING+="REACT_APP_IDP_REALM: \"${REACT_APP_IDP_REALM:-null}\","
ENVSTRING+="REACT_APP_IDP_CLIENT_ID: \"${REACT_APP_IDP_CLIENT_ID:-null}\","

sed -i -e "s%/\*@preserve __ENV_INJECT_MARK__\*/%$ENVSTRING%g" ./build/index.html