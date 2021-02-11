import Keycloak from "keycloak-js";

// Setup Keycloak instance as needed
// Pass initialization options as required or leave blank to load from 'keycloak.json'
const keycloak = Keycloak({
  url: process.env.REACT_APP_IDP_URL,
  realm: process.env.REACT_APP_IDP_REALM || "",
  clientId: process.env.REACT_APP_IDP_CLIENT_ID || "",
});

export default keycloak;
