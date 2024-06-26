import Keycloak from "keycloak-js";

import { idpClientId, idpRealm, idpUrl } from "./js/environment";

// Setup Keycloak instance as needed
// Pass initialization options as required or leave blank to load from 'keycloak.json'
const keycloak = Keycloak({
  url: idpUrl,
  realm: idpRealm,
  clientId: idpClientId,
});

export default keycloak;
