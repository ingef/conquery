import { initializeEnvironment } from "../../lib/js/environment";

export default function setupEnvironment() {
  initializeEnvironment({
    basename: "/",
    // All API requests will be mocked, so this value doesn't really matter
    apiUrl: "http://localhost:8080",
    isProduction: true,
    disableLogin: false
  });
}
