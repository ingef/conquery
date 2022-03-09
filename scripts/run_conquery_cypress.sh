#!/bin/bash
"./scripts/run_backend_cypress.sh" &
sleep 15
"./scripts/load_test_data_cypress.sh"
"./scripts/run_frontend_cypress.sh"
