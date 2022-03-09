#!/bin/bash

jar="./executable/target/executable*.jar"
config="./frontend/cypress/support/backend_config.json"

java -jar $jar standalone $config
