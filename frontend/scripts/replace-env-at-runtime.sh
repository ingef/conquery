#!/bin/sh

usage=`cat <<-EOF
Replace the /*@preserve __ENV_INJECT_MARK__*/ marker in an conquery index.html with environment variables.

Usage:
$0 <index.html>
EOF
`

# Check for one or two arguments or exit 
if  [ \( "$#" -ne 1 \) ] 
then
    echo "$usage"
    exit 1
fi

# Build the env string
ENVSTRING=""
ENVSTRING="${ENVSTRING}REACT_APP_API_URL: \"${REACT_APP_API_URL:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_DISABLE_LOGIN: \"${REACT_APP_DISABLE_LOGIN:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_LANG: \"${REACT_APP_LANG:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_BASENAME: \"${REACT_APP_BASENAME:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_IDP_ENABLE: \"${REACT_APP_IDP_ENABLE:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_IDP_URL: \"${REACT_APP_IDP_URL:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_IDP_REALM: \"${REACT_APP_IDP_REALM:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_IDP_CLIENT_ID: \"${REACT_APP_IDP_CLIENT_ID:-null}\","
ENVSTRING="${ENVSTRING}REACT_APP_BLURRED_ENTITY_DEFAULT: \"${REACT_APP_BLURRED_ENTITY_DEFAULT:-null}\","


# Replace the marker
sed -i -e "s%/\*@preserve __ENV_INJECT_MARK__\*/%$ENVSTRING%g" $1
