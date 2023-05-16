import {
  format,
  parse,
  addQuarters,
  lastDayOfQuarter,
  addMonths,
  endOfMonth,
  isValid,
  differenceInCalendarDays,
  formatDistance,
} from "date-fns";
import { de, enGB } from "date-fns/locale";
import i18next from "i18next";
import { useTranslation } from "react-i18next";

// To save the date in this format in the state
const DATE_FORMAT = "yyyy-MM-dd";

export const useDateLocale = () => {
  const { i18n } = useTranslation();

  return i18n.language === "de" ? de : enGB;
};

export const useFormatDateDistance = () => {
  const locale = useDateLocale();

  return (d1: Date, d2: Date, withSuffix: boolean = false) =>
    formatDistance(d1, d2, { locale: locale, addSuffix: withSuffix });
};

export const formatStdDate = (date: Date) => {
  return formatDate(date, DATE_FORMAT);
};

export const formatDate = (date: Date, dateFormat: string) => {
  return date ? format(date, dateFormat) : "";
};

export const formatDateFromState = (
  dateString: string,
  dateFormat: string,
): string => {
  const date = parseDate(dateString, DATE_FORMAT);

  return date !== null ? format(date, dateFormat) : dateString;
};

export const parseDateToState = (date: Date) => {
  return format(date, DATE_FORMAT);
};

export const parseDate = (dateString: string, dateFormat: string) => {
  // Otherwise 15.06.2 parses years as 15.06.0002
  if (!dateString || dateString.length !== dateFormat.length) return null;

  const date = parse(dateString, dateFormat, new Date());

  return isValid(date) ? date : null;
};

export const parseStdDate = (dateString: string) => {
  return parseDate(dateString, DATE_FORMAT);
};

const DATE_PATTERN = {
  raw: /(^\d{8})$/,
  year: /^[yj][.]*(\d{4})$/,
  quarter_year: /^[q]([1-4]).(\d{4})$/,
  month_year: /^[m](1[0-2]|[1-9]).(\d{4})$/,
};

function handleRaw(
  what: "min" | "max",
  value: string,
  displayDateFormat: string,
) {
  const denseFormat = displayDateFormat.replace(/[-/.]/g, "");
  // Assuming the format consists of 2 M, 2 d, and 2 y

  const dIdx = denseFormat.indexOf("d");
  const d = value.substring(dIdx, dIdx + 2);

  const mIdx = denseFormat.indexOf("M");
  const m = value.substring(mIdx, mIdx + 2);

  const yIdx = denseFormat.indexOf("y");
  const y = value.substring(yIdx, yIdx + 4);

  let date: Date | null = new Date(parseInt(y), parseInt(m) - 1, parseInt(d));

  date = isValid(date) ? date : null;

  if (what === "min") return { min: date, max: null };
  else return { min: null, max: date };
}

function handleYear(value: string) {
  const match = DATE_PATTERN.year.exec(value);
  if (!match) {
    throw new Error("Quarter.year should match at this point");
  }
  const year = parseInt(match[1]);

  const min = new Date(year, 0, 1);
  const max = new Date(year, 11, 31);

  return { min, max };
}

function handleQuarter(value: string) {
  const match = DATE_PATTERN.quarter_year.exec(value);

  if (!match) {
    throw new Error("Quarter.year should match at this point");
  }

  const quarter = parseInt(match[1]);
  const year = parseInt(match[2]);

  const min = addQuarters(new Date(year, 0, 1), quarter - 1);
  const max = lastDayOfQuarter(addQuarters(new Date(year, 0, 1), quarter - 1));

  return { min, max };
}

function handleMonth(value: string) {
  const match = DATE_PATTERN.month_year.exec(value);

  if (!match) {
    throw new Error("Month.year should match at this point");
  }

  const month = parseInt(match[1]);
  const year = parseInt(match[2]);

  const min = addMonths(new Date(year, 0, 1), month - 1);
  const max = endOfMonth(addMonths(new Date(year, 0, 1), month - 1));

  return { min, max };
}

export interface DateStringMinMax {
  min: string | null; // in DATE_FORMAT
  max: string | null; // in DATE_FORMAT
}

export const getDateStringFromShortcut = (
  what: "min" | "max",
  value: string,
  displayDateFormat: string,
): DateStringMinMax => {
  const date = testRegexes(what, value, displayDateFormat);

  return {
    min: date.min ? format(date.min, DATE_FORMAT) : null,
    max: date.max ? format(date.max, DATE_FORMAT) : null,
  };
};

interface DateMinMax {
  min: Date | null;
  max: Date | null;
}

export const testRegexes = (
  what: "min" | "max",
  value: string,
  displayDateFormat: string,
): DateMinMax => {
  switch (true) {
    case DATE_PATTERN.raw.test(value):
      return handleRaw(what, value, displayDateFormat);
    case DATE_PATTERN.year.test(value):
      return handleYear(value);
    case DATE_PATTERN.quarter_year.test(value):
      return handleQuarter(value);
    case DATE_PATTERN.month_year.test(value):
      return handleMonth(value);
    default:
      return { min: null, max: null };
  }
};

export function getDiffInDays(d1: Date, d2: Date) {
  return Math.abs(differenceInCalendarDays(d1, d2)) + 1;
}

/**
 * A custom string format we're using that allows a list of day intervals
 * we call this format a "date set"
 *
 * Open end intervals are supported using -∞ and +∞
 *
 * Examples:
 * dateStr: "{2020-01-01/2020-01-03}"
 * dateStr: "{2020-01-01/2020-01-31,2020-02-01/2020-02-28}"
 * dateStr: "{-∞/2020-01-31,2020-02-01/2020-02-28,2020-05-01/+∞}"
 *
 * @param dateStr a date set
 * @returns
 * - the first date of the first interval or null in case of infinity
 * - the last date of the last interval or null in case of infinity
 */
export function getFirstAndLastDateOfRange(dateRangeStr: string): {
  first: Date | null;
  last: Date | null;
} {
  const dateStrTrimmed = dateRangeStr.slice(1, dateRangeStr.length - 1);

  const ranges = dateStrTrimmed.split(",");
  const first = parseStdDate(ranges[0].split("/")[0]);
  const last = parseStdDate(ranges[ranges.length - 1].split("/")[1]);

  return { first, last };
}

export function getMonthName(date: Date): string {
  const locale = i18next.language === "de" ? de : enGB;
  return format(date, "LLLL", { locale });
}

export function getMonthNames(): string[] {
  return [...Array(12).keys()].map((month) => {
    const date = new Date();
    date.setMonth(month);
    return getMonthName(date);
  });
}
