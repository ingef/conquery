#!/bin/bash

"./scripts/run_backend_cypress.sh" &
"./scripts/load_test_data_cypress.sh"
"./scripts/run_frontend_cypress.sh"
