#!/bin/bash

# This is for the cypress-io/github-action so it can call the actual script without referencing a file outside the working dir

pushd ..
./scripts/run_conquery_cypress.sh
popd