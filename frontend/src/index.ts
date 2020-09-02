import { de } from "date-fns/locale";
import { enGB } from "date-fns/locale";

import conquery from "./js";

import { initializeLocalization } from "./js/localization";
import translationsDe from "./localization/de.json";
import translationsEn from "./localization/en.json";

import StandardQueryEditorTab from "./js/standard-query-editor";
import TimebasedQueryEditorTab from "./js/timebased-query-editor";
import FormsTab from "./js/external-forms";

import { theme } from "./app-theme";

import "./app-styles.sass";

const isProduction = process.env.NODE_ENV === "production";
const disableLogin = !!process.env.REACT_APP_DISABLE_LOGIN;
const LANG = process.env.REACT_APP_LANG;

if (!LANG || LANG === "de") {
  initializeLocalization("de", de, translationsDe);
} else {
  initializeLocalization("en", enGB, translationsEn);
}

const MOCK_API_URL = "http://localhost:8001";

const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? "/" // Possibly: Run under a subpath in production
    : "/",
  apiUrl: !!process.env.REACT_APP_API_URL
    ? process.env.REACT_APP_API_URL
    : isProduction
    ? ""
    : MOCK_API_URL,
  disableLogin,
};

const tabs = [StandardQueryEditorTab, TimebasedQueryEditorTab, FormsTab];

conquery(environment, tabs, theme);
