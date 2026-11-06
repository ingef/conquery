#!/bin/sh

set -e

usage=$(cat <<-EOF
Replace the /*@preserve __ENV_INJECT_MARK__*/ marker in an conquery index.html with environment variables.

Usage:
$0 <index.html>
EOF
)

# Check for one or two arguments or exit 
if  [ \( "$#" -ne 1 \) ] 
then
    echo "$usage"
    exit 1
fi

# Build the env string: get all envvars prefixed with REACT_APP_ and convert them to key value pairs for JS
ENVSTRING=$(env | grep '^REACT_APP_' | sed 's/=\(.*\)/: "\1"/' | tr '\n' ',')

echo "$ENVSTRING"

# Replace the marker
sed -i -e "s%/\*@preserve __ENV_INJECT_MARK__\*/%$ENVSTRING%g" "$1"
