#!/bin/bash

set -x

mvn clean

mvn -T 1C package -Dmaven.test.skip=true -DskipTests -Drevision=2.0.0-`git describe --tags` -pl executable -am
