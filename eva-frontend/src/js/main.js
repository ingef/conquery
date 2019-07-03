// @flow

import { de } from "date-fns/locale";

import evaStrings from "../localization/de.yml";
import ExternalFormsTab from "./external-forms";

import conquery from "conquery/lib/js";
import { initializeLocalization } from "conquery/lib/js/localization";
import translations from "conquery/lib/localization/de.yml";
import StandardQueryEditorTab from "conquery/lib/js/standard-query-editor";
import TimebasedQueryEditorTab from "conquery/lib/js/timebased-query-editor";

import theme from "../styles/theme";

require("../styles/styles.sass");
require("../images/favicon.png");

initializeLocalization("de", de, translations, evaStrings);

const isProduction = process.env.NODE_ENV === "production";
const environment = {
  isProduction: isProduction,
  basename: isProduction ? "/app" : "https://37c21eb1.eu.ngrok.io",
  apiUrl: "/api"
};

const tabs = [
  StandardQueryEditorTab,
  TimebasedQueryEditorTab,
  ExternalFormsTab
];

conquery(environment, tabs, theme);
