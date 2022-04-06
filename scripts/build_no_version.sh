#!/bin/bash

mvn -T 1C clean package -Dmaven.test.skip=true -DskipTests -pl executable -am
