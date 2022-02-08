#!/bin/env bash
proj_dir=$(readlink -f `dirname $0`/..)

pushd "$proj_dir/frontend"

npx serve -s build -l 3000

popd