#!/bin/env bash
script_dir=`dirname $0`

jar="$(readlink -f $script_dir/../executable/target/executable*.jar)"
config="$(readlink -f $script_dir/../frontend/cypress/support/backend_config.json)"

java -jar $jar standalone $config
