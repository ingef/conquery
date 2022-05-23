#!/bin/bash

echo "Starting full stack for e2e testing ..."

"./scripts/run_e2e_backend.sh" & "./scripts/load_e2e_data.sh"

"./scripts/run_e2e_frontend.sh"
