#!/bin/env bash
proj_dir=$(readlink -f `dirname $0`/..)

"$proj_dir/scripts/run_backend_cypress.sh" &
"$proj_dir/scripts/run_frontend_cypress.sh"
