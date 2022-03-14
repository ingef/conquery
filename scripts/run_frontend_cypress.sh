#!/bin/bash
proj_dir=$(readlink -f `dirname $0`/..)

pushd "$proj_dir/frontend"

yarn serve 

popd