# Custom Forms

Customs are provided through conquery by a form backend.
The form backend must implement a specific API and be configured for the backend at start-up.
The form backend usually interacts with conquery as a normal client using the same API as the frontend.

## Form Backend Implementation

The form backend must
implement [this OpenAPI spec](../backend/src/main/resources/com/bakdata/conquery/external/openapi-form-backend.yaml) as
a server.

## Conquery Backend Configuration

In the conquery configuration JSON the `plugins`-section needs to be adjusted:

```json
{
  ...,
  "plugins": [
    ...,
    {
      "type": "FORM_BACKEND",
      "id": "some_form_backend",
      "baseURI": "http://localhost:8000",
      "conqueryApiUrl": "http://localhost:8080",
      "authentication": {
        "type": "API_KEY",
        "apiKey": "secret_api_key"
      },
      "serviceUserPermissions": [
        "concepts:READ:${DATASET}.concept"
      ]
    }
  ]
}
```

This tells conquery, there is a form backend at `http://localhost:8000` and it authenticates itself using the `apiKey`.

When a form is submitted, conquery creates a temporary service user for the form backend. The service user inherits the
submitting user's permissions, is automatically granted download permission for the active dataset, and receives the
additional permissions listed in `serviceUserPermissions`. These permissions are granted only to the service user and do
not modify the submitting user's permissions.

Entries in `serviceUserPermissions` support the `${DATASET}` template variable. Conquery replaces it with the active
dataset's ID when creating the service user. For example, for a dataset with the ID `my-dataset`, the configured permission
`concepts:READ:${DATASET}.concept` is rendered as
`concepts:READ:my-dataset.concept`.
