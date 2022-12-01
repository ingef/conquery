#!/bin/bash

set -x

git_descibe=`git describe --tags`

mvn -T 1C clean package -Dmaven.test.skip=true -DskipTests -Drevision=${git_descibe#v} -pl executable -am
