import { de } from "date-fns/locale";

import { initializeLocalization } from "./js/localization";
import translations from "./localization/de.json";

import conquery from "./js";

import StandardQueryEditorTab from "./js/standard-query-editor";
import TimebasedQueryEditorTab from "./js/timebased-query-editor";
import FormsTab from "./js/external-forms";

import theme from "./app-theme";

import "./app-styles.sass";

initializeLocalization("de", de, translations);

const isProduction = process.env.NODE_ENV === "production";
const disableLogin = !!process.env.DISABLE_LOGIN;

const MOCK_API_URL = "http://localhost:8001";

const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? "/" // Possibly: Run under a subpath in production
    : "/",
  apiUrl: !!process.env.API_URL ? process.env.API_URL : MOCK_API_URL,
  disableLogin
};

const tabs = [StandardQueryEditorTab, TimebasedQueryEditorTab, FormsTab];

conquery(environment, tabs, theme);
