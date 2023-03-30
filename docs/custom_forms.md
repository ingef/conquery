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
      }
    }
  ]
}
```

This tells conquery, there is a form backend at `http://localhost:8000` and it authenticates itself using the `apiKey`.
