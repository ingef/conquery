#!/bin/bash

mvn clean initialize -P setVersion

mvn -T 1C package -DskipTests -pl executable -am
