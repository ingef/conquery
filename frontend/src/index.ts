import { theme } from "./app-theme";
import conquery from "./js";
import { language } from "./js/environment";
import i18next from "./js/localization/i18next";
import translationsDe from "./localization/de.json";
import translationsEn from "./localization/en.json";

i18next.addResourceBundle("de", "translation", translationsDe, true, true);
i18next.addResourceBundle("en", "translation", translationsEn, true, true);
i18next.changeLanguage(language);

conquery({ theme });
