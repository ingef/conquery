import "./app-styles.sass";
import { theme } from "./app-theme";
import conquery from "./js";
import { language, CustomEnvironment } from "./js/environment";
import FormsTab from "./js/external-forms";
import i18next from "./js/localization/i18next";
import { TabT } from "./js/pane/types";
import StandardQueryEditorTab from "./js/standard-query-editor";
import TimebasedQueryEditorTab from "./js/timebased-query-editor";
import translationsDe from "./localization/de.json";
import translationsEn from "./localization/en.json";

i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.addResourceBundle("en", "translation", translationsEn, true, true);
i18next.changeLanguage(language);

const customEnvironment: CustomEnvironment = {};

const tabs: TabT[] = [
  StandardQueryEditorTab,
  TimebasedQueryEditorTab,
  FormsTab,
];

conquery({ theme, tabs, customEnvironment });
