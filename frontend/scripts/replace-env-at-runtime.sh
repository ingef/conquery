#!/bin/sh

usage=`cat <<-EOF
Replace the /*@preserve __ENV_INJECT_MARK__*/ marker in an conquery index.html with environment variables.
If an optional .env-file is supplied it will be read before subsitution. 

Usage:
$0 <index.html> [.env-file]
EOF
`

# Check for one or two arguments or exit 
if [[ ( $# < 1 || $# > 2 )]]
then
    echo "$usage"
    exit 1
fi

# Check if env file is provided and readable
if [[ $# == 2 && -r $2 ]]
then
    # if so, we asume it is an env-file and we source it
    . $2
fi

# Build the env string
ENVSTRING=""
ENVSTRING+="REACT_APP_API_URL: \"${REACT_APP_API_URL:-null}\","
ENVSTRING+="REACT_APP_DISABLE_LOGIN: \"${REACT_APP_DISABLE_LOGIN:-null}\"," 
ENVSTRING+="REACT_APP_LANG: \"${REACT_APP_LANG:-null}\","
ENVSTRING+="REACT_APP_BASENAME: \"${REACT_APP_BASENAME:-null}\","
ENVSTRING+="REACT_APP_IDP_ENABLE: \"${REACT_APP_IDP_ENABLE:-null}\","
ENVSTRING+="REACT_APP_IDP_URL: \"${REACT_APP_IDP_URL:-null}\","
ENVSTRING+="REACT_APP_IDP_REALM: \"${REACT_APP_IDP_REALM:-null}\","
ENVSTRING+="REACT_APP_IDP_CLIENT_ID: \"${REACT_APP_IDP_CLIENT_ID:-null}\","

# Replace the marker
sed -i -e "s%/\*@preserve __ENV_INJECT_MARK__\*/%$ENVSTRING%g" $1