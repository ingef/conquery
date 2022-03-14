#!/bin/bash
proj_dir=$(readlink -f `dirname $0`/..)

"$proj_dir/scripts/run_backend_cypress.sh" &
sleep 15
"$proj_dir/scripts/load_test_data_cypress.sh"
"$proj_dir/scripts/run_frontend_cypress.sh"
