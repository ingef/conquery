// @flow

import moment from "moment";

export const formatDate = (dateString: String) => {
  return moment(dateString).format(moment.localeData().longDateFormat("L"));
};

export const duration = (value, units: string, format: string) => {
  return moment.duration(value, units).format(format, { trim: false });
};

const DATE_PATTERN = {
  year: /^\.(\d{4})$/,
  quarter_year: /^[q]([1-4]).(\d{4})$/,
  month_year: /^[m](1[0-2]|[1-9]).(\d{4})$/,
  date: /^\d{8}$/
};

export const specificDatePattern = value => {
  var minDate, maxDate, year, match;
  switch (true) {
    case DATE_PATTERN.year.test(value):
      year = parseInt(DATE_PATTERN.year.exec(value)[1]);
      minDate = moment([year, 0, 1]);
      maxDate = moment([year, 11, 31]);
      break;
    case DATE_PATTERN.quarter_year.test(value):
      match = DATE_PATTERN.quarter_year.exec(value);
      const quarter = parseInt(match[1]);
      year = parseInt(match[2]);
      minDate = moment([year, 0, 1]).quarter(quarter);
      maxDate = moment([year, 0, 1])
        .quarter(quarter + 1)
        .subtract(1, "day");
      break;
    case DATE_PATTERN.month_year.test(value):
      match = DATE_PATTERN.month_year.exec(value);
      const month = parseInt(match[1]);
      year = parseInt(match[2]);
      minDate = moment([year, 0, 1]).month(month - 1);
      maxDate = moment([year, 0, 1])
        .month(month)
        .subtract(1, "day");
      break;
    default:
      break;
  }

  return { minDate, maxDate };
};

export const parseDatePattern = (value, pattern) => {
  if (value && DATE_PATTERN.date.test(value)) return moment(value, "DDMMYYYY");
};
