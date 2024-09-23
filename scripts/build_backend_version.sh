#!/bin/bash

if [ -z $1 ]
then
	echo "No version supplied. Try to extract it using 'git describe --tags'"
	if git_descibe=`git describe --tags`
	then
		version=${git_descibe#v}
		echo "Found version $version"
	else
		echo "Could not determine a version"
		exit 1
	fi
else
	version=$1
fi


set -x

mvn -T 1C clean package -Dmaven.test.skip=true -DskipTests "-Drevision=$version" -pl executable -am
