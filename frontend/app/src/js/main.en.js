// @flow

import { initializeLocalization } from "../../../lib/js/localization";
import translations from "../../../lib/localization/en.yml";
import conqueryTranslations from "../localization/en.yml";

initializeLocalization("en", translations, conqueryTranslations);

require("./main");
