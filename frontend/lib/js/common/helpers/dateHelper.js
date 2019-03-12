// @flow

import {
  format,
  parse,
  addQuarters,
  lastDayOfQuarter,
  addMonths,
  endOfMonth
} from "date-fns";

export const formatDate = (date, dateFormat) =>
  date && isValidDate(date) ? format(date, dateFormat) : null;

export const parseDate = (date, dateFormat) =>
  date ? parse(date, dateFormat, new Date()) : null;

export const parseRawDate = value => parseDate(value, "ddMMyyyy");

export const isValidDate = date => {
  // https://stackoverflow.com/questions/1353684/detecting-an-invalid-date-date-instance-in-javascript
  if (Object.prototype.toString.call(date) === "[object Date]") {
    // it is a date
    return !isNaN(date.getTime());
  }

  return false;
};

const DATE_PATTERN = {
  year: /^[y](\d{4})$/,
  quarter_year: /^[q]([1-4]).(\d{4})$/,
  month_year: /^[m](1[0-2]|[1-9]).(\d{4})$/
};

export const parseDateFromShortcut = value => {
  let minDate, maxDate, year, match;

  switch (true) {
    case DATE_PATTERN.year.test(value):
      year = parseInt(DATE_PATTERN.year.exec(value)[1]);

      minDate = new Date(year, 0, 1);
      maxDate = new Date(year, 11, 31);

      break;
    case DATE_PATTERN.quarter_year.test(value):
      match = DATE_PATTERN.quarter_year.exec(value);

      const quarter = parseInt(match[1]);
      year = parseInt(match[2]);

      minDate = addQuarters(new Date(year, 0, 1), quarter - 1);
      maxDate = lastDayOfQuarter(
        addQuarters(new Date(year, 0, 1), quarter - 1)
      );

      break;
    case DATE_PATTERN.month_year.test(value):
      match = DATE_PATTERN.month_year.exec(value);

      const month = parseInt(match[1]);
      year = parseInt(match[2]);

      minDate = addMonths(new Date(year, 0, 1), month - 1);
      maxDate = endOfMonth(addMonths(new Date(year, 0, 1), month - 1));

      break;
    default:
      break;
  }

  return { min: minDate, max: maxDate };
};
