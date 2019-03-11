// @flow

import conquery from "../../../lib/js";
import { StandardQueryEditorTab } from "../../../lib/js/standard-query-editor";
import { TimebasedQueryEditorTab } from "../../../lib/js/timebased-query-editor";

import theme from "../styles/theme";

require("../styles/styles.sass");
require("../images/favicon.png");

const isProduction = process.env.NODE_ENV === "production";
const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? "/" // Possibly: Run under a subpath in production
    : "/",
  apiUrl: "/api"
};

const tabs = [StandardQueryEditorTab, TimebasedQueryEditorTab];

conquery(environment, tabs, theme);
