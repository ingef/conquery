# Configure Authentication with OAuth2/OpenId Connect

We will discuss how to configure authentication with Keycloak, an open-source identity and access management tool, in
Conquery.
Using this configuration, Conquery's frontend acts as a public client, while the backend validates the provided tokens
offline.

### Prerequisites:

Before proceeding with the configuration process, ensure that you have the following prerequisites:

Conquery is installed and running on your system.  
The Keycloak server is installed and running on your system.  
You have administrative access to both Conquery and Keycloak.

### Configuration

To configure authentication with Keycloak in the Conquery project, follow these steps:

**Step 1: Create a Keycloak Realm (optional)**
The first step is to create a new Keycloak realm dedicated to Conquery. To create a new realm, follow these steps:

- Login to the Keycloak Administration Console using your administrative credentials.
- Click on the "Add Realm" button on the left sidebar.
- Enter a name for the new realm (e.g., Conquery).
- Click on the "Create" button.

Finally, you can retrieve the well-known url to the new realm which will be later referenced by `<realm-well-known-url>`
.

**Step 2: Configure Keycloak Client**

The next step is to configure a Keycloak client for Conquery. To do this, follow these steps:

- In the Keycloak Administration Console, navigate to the Conquery realm.
- Click on the "Clients" tab in the left sidebar and then click on the "Create" button.
- Enter a name for the client (e.g., conquery-frontend).
- Set the "Client Protocol" to "openid-connect".
- Set the "Access Type" to "public".
- Set the "Valid Redirect URIs" to the Conquery frontend URL (e.g., http://localhost:8000).
- Click on the "Save" button.

**Step 3: Configure Conquery**

The final step is to configure Conquery to use Keycloak for authentication. To do this, follow these steps:

- Open the Conquery configuration file (e.g., config.json) in a text editor to configure the backend.
  Add the following lines to the configuration file:
  ```json
    "authenticationRealms": [
        {
            "type" : "JWT_PKCE_REALM",
            "wellKnownEndpoint" : "<realm-well-known-url>",
            "client" : "<client-name>"
        }
    ]
  ```
  Replace the `<realm-well-known-url>` and `<client-name>` with the names you used in Steps 1 and 2.

- Save the changes to the configuration file and restart the Conquery server.
- Assuming you are running the frontend in the provided docker container, you need to set the following environment
  variables, when running the container:
  ```bash
  docker run \
    --env REACT_APP_API_URL=<conquery-backend-url> \
    --env REACT_APP_IDP_REALM=<realm-name> \
    --env REACT_APP_IDP_CLIENT_ID=<client-name> \
    --env REACT_APP_DISABLE_LOGIN=true \
    --env REACT_APP_IDP_URL=<keycloak-server-url> \
    --env REACT_APP_IDP_ENABLE=true \
    --publish 8000:80 \
    ghcr.io/ingef/conquery-frontend
  ```

By following these steps, you can ensure that your Conquery instance is secure and protected from unauthorized access.