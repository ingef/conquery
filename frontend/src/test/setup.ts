import axios from "axios";
import { initializeEnvironment } from "../js/environment";
import httpAdapter from "axios/lib/adapters/http";

axios.defaults.adapter = httpAdapter;

initializeEnvironment({
  basename: "/",
  // All API requests will be mocked, so this value doesn't really matter
  apiUrl: "http://localhost:8001",
  isProduction: true,
  disableLogin: false,
});
