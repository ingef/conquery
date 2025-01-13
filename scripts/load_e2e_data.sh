#!/bin/bash

admin_api="http://localhost:8081/admin"
h_ct="content-type:application/json"
h_auth="authorization: Bearer user.SUPERUSER"

echo
echo "Loading data into backend for e2e testing"

echo "Waiting to $admin_api come up."
until $(curl --output /dev/null --silent --head -H "$h_auth" --fail $admin_api/users/); do
    printf '.'
    sleep 5
done

echo "Preprocess test data"
java -jar ./executable/target/executable*.jar preprocess --in cypress/support/test_data/ --out cypress/support/test_data/ --desc cypress/support/test_data/*.import.json

# Create users
echo "Creating users and permissions"
curl --fail -X POST  "$admin_api/users/" -H "$h_ct" -H "$h_auth" -d '{"name": "user1", "label": "User1"}'

curl --fail -X POST  "$admin_api/users/" -H "$h_ct" -H "$h_auth" -d '{"name": "user2", "label": "User2"}'
curl --fail -X POST  "$admin_api/permissions/user.user2" -H "$h_ct" -H "$h_auth" -d 'datasets:read,download,preserve_id:dataset1'
curl --fail -X POST  "$admin_api/permissions/user.user2" -H "$h_ct" -H "$h_auth" -d 'concepts:read:*'

echo "Creating dataset"
# Create dataset
curl --fail -X POST  "$admin_api/datasets/" -H "$h_ct" -H "$h_auth" -d '{"name": "dataset1", "label": "Dataset1"}'
sleep 3

echo "Creating mappings"
curl --fail -X POST  "$admin_api/datasets/dataset1/internToExtern" -H "$h_ct" -H "$h_auth" -d "@./cypress/support/test_data/mapping.mapping.json"
sleep 3

echo "Creating secondary ids"
curl --fail -X POST  "$admin_api/datasets/dataset1/secondaryId" -H "$h_ct" -H "$h_auth" -d "@./cypress/support/test_data/sid.secondaryId.json"
sleep 1

 # TODO secondary ID
echo "Creating tables"
for table_json in `ls ./cypress/support/test_data/*.table.json`
do
    curl --fail -X POST  "$admin_api/datasets/dataset1/tables" -H "$h_ct" -H "$h_auth" -d "@$table_json"
done
sleep 3

echo "Creating concepts"
for concept_json in `ls ./cypress/support/test_data/*.concept.json`
do
    curl --fail -X POST  "$admin_api/datasets/dataset1/concepts" -H "$h_ct" -H "$h_auth" -d "@$concept_json"
done

echo "Upload test data"
for cqpp in `ls ./cypress/support/test_data/*.cqpp`
do
    curl --fail -X POST --compressed "$admin_api/datasets/dataset1/cqpp" -H "content-type:application/octet-stream" -H "$h_auth" --data-binary "@$cqpp"
done

echo "Init Matching Stats and Search"
curl --fail -X POST  "$admin_api/datasets/dataset1/update-matching-stats" -H "$h_ct" -H "$h_auth"

echo "Done loading data"
