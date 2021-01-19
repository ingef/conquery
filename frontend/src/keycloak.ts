import Keycloak from "keycloak-js";

const keycloakConfig = {
  url: "http://localhost:8080/auth",
  realm: "Demo",
  clientId: "react-app",
};

// Setup Keycloak instance as needed
// Pass initialization options as required or leave blank to load from 'keycloak.json'
const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
