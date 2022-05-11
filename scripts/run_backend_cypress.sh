#!/bin/bash

jar="./executable/target/executable-0.0.0-SNAPSHOT.jar"
config="./frontend/cypress/support/backend_config.json"

if [ ! -e $jar ]
then
    echo "Backend executable not found. Trying to build it"
    ./scripts/build_no_version.sh
fi

java -jar $jar standalone $config
