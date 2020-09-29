import _T from "i18n-react";
import { Locale } from "date-fns";


import { mergeDeep } from "../common/helpers";

export const T = _T;

type AvailableLocale = "en" | "de";

let i18nLocale: AvailableLocale | null = null;
let dateFnsLocale: AvailableLocale | null = null;

export const initializeLocalization = (
  locale: AvailableLocale,
  dateLocale: Locale,
  ...texts
) => {
  T.setTexts(mergeDeep(...texts));

  i18nLocale = locale;
  dateFnsLocale = dateLocale;
};

export const getDateLocale = () => {
  return dateFnsLocale;
};

export const getLocale = () => {
  return i18nLocale || "de";
};
