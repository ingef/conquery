// @flow

import { enGB } from "date-fns/locale";

import { initializeLocalization } from "../../../lib/js/localization";
import translations from "../../../lib/localization/en.yml";

initializeLocalization("en", enGB, translations);

require("./main");
