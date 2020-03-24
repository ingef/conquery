// @flow

import conquery from "../../../lib/js";
import StandardQueryEditorTab from "../../../lib/js/standard-query-editor";
import TimebasedQueryEditorTab from "../../../lib/js/timebased-query-editor";
import FormsTab from "../../../lib/js/external-forms";

import theme from "../styles/theme";

require("../styles/styles.sass");
require("../images/favicon.png");

const isProduction = process.env.NODE_ENV === "production";
const disableLogin = !!process.env.DISABLE_LOGIN;

const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? "/" // Possibly: Run under a subpath in production
    : "/",
  // apiUrl: !!process.env.API_URL ? process.env.API_URL : "",
  apiUrl: "http://localhost:3001",
  disableLogin
};

const tabs = [StandardQueryEditorTab, TimebasedQueryEditorTab, FormsTab];

conquery(environment, tabs, theme);
