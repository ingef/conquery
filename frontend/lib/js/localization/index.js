import _T from "i18n-react";

import { mergeDeep } from "../common/helpers";

export const T = _T;

let dateFnsLocale = null;

export const initializeLocalization = (language, dateLocale, ...texts) => {
  T.setTexts(mergeDeep(...texts));

  dateFnsLocale = dateLocale;
};

export const getDateLocale = () => {
  return dateFnsLocale;
};
