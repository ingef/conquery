import "./app-styles.sass";
import { theme } from "./app-theme";
import conquery from "./js";
import { Environment } from "./js/environment";
import FormsTab from "./js/external-forms";
import i18next from "./js/localization/i18next";
import { TabT } from "./js/pane/types";
import StandardQueryEditorTab from "./js/standard-query-editor";
import TimebasedQueryEditorTab from "./js/timebased-query-editor";
import translationsDe from "./localization/de.json";
import translationsEn from "./localization/en.json";

const isProduction = process.env.NODE_ENV === "production";
const disableLogin = process.env.REACT_APP_DISABLE_LOGIN === "true";
const enableIDP = process.env.REACT_APP_IDP_ENABLE === "true";
const LANG = process.env.REACT_APP_LANG;

i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.addResourceBundle("en", "translation", translationsEn, true, true);
i18next.changeLanguage(LANG === "de" ? "de" : "en");

const environment: Environment = {
  isProduction: isProduction,
  basename: isProduction
    ? "/" // Possibly: Run under a subpath in production
    : "/",
  apiUrl: process.env.REACT_APP_API_URL || "",
  disableLogin,
  enableIDP,
};

const tabs: TabT[] = [
  StandardQueryEditorTab,
  TimebasedQueryEditorTab,
  FormsTab,
];

conquery({ environment, tabs, theme });
