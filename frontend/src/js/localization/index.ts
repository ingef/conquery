// @flow

import _T from "i18n-react";

import { mergeDeep } from "../common/helpers";

export const T = _T;

let i18nLocale = null;
let dateFnsLocale = null;

export const initializeLocalization = (locale, dateLocale, ...texts) => {
  T.setTexts(mergeDeep(...texts));

  i18nLocale = locale;
  dateFnsLocale = dateLocale;
};

export const getDateLocale = () => {
  return dateFnsLocale;
};

export const getLocale = () => {
  return i18nLocale;
};
