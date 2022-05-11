#!/bin/bash

jar="./executable/target/executable-0.0.0-SNAPSHOT.jar"
config="./cypress/support/backend_config.json"

echo ""

if [ ! -e $jar ]
then
    echo "Backend executable not found. Trying to build it"
    ./scripts/build_backend_no_version.sh
else
    echo "Backend executable found: $jar"

fi

echo ""
echo "Starting backend server"
java -jar $jar standalone $config
