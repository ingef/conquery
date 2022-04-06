#!/bin/bash

admin_api="http://localhost:8081/admin"
h_ct="content-type:application/json"
h_auth="authorization: Bearer user.SUPERUSER@SUPERUSER"

echo "Waiting to $admin_api come up."
until $(curl --output /dev/null --silent --head -H "$h_auth" --fail $admin_api/users/); do
    printf '.'
    sleep 5
done

# create users
curl -X POST  "$admin_api/users/" -H "$h_ct" -H "$h_auth" -d '{"name": "user1", "label": "User1"}'

curl -X POST  "$admin_api/users/" -H "$h_ct" -H "$h_auth" -d '{"name": "user2", "label": "User2"}'
curl -X POST  "$admin_api/permissions/user.user2" -H "$h_ct" -H "$h_auth" -d 'datasets:read,download,preserve_id:dataset1'
curl -X POST  "$admin_api/permissions/user.user2" -H "$h_ct" -H "$h_auth" -d 'concepts:read:*'

#create dataset
curl -X POST  "$admin_api/datasets/" -H "$h_ct" -H "$h_auth" -d '{"name": "dataset1", "label": "Dataset1"}'
sleep 3
 # TODO secondary ID
curl -X POST  "$admin_api/datasets/dataset1/tables" -H "$h_ct" -H "$h_auth" -d "@./frontend/cypress/support/test_data/all_types.table.json"
sleep 3
curl -X POST  "$admin_api/datasets/dataset1/concepts" -H "$h_ct" -H "$h_auth" -d "@./frontend/cypress/support/test_data/all_types.concept.json"