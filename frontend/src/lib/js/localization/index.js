import _T from "i18n-react";
import moment from "moment";
import momentDurationFormatSetup from "moment-duration-format";

import { mergeDeep } from "../common/helpers";

export const T = _T;

export const initializeLocalization = (language, ...texts) => {
  T.setTexts(mergeDeep(...texts));

  // Set moment locale
  momentDurationFormatSetup(moment);
  moment.locale(language);
};
