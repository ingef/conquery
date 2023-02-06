import axios from "axios";
import httpAdapter from "axios/lib/adapters/http";

import { initializeEnvironment } from "../js/environment";

axios.defaults.adapter = httpAdapter;

initializeEnvironment({});
