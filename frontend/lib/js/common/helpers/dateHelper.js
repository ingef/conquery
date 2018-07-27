// @flow

import moment         from "moment";

export const formatDate = (dateString: String) => {
  return moment(dateString).format(moment.localeData().longDateFormat("L"))
};

export const duration = (value, units: string, format: string) => {
  return moment.duration(value, units).format(format, {trim: false});
};

const DATE_PATTERN = {
  year: /^\.(\d{4})/, // e.g. .2018 > min: 01.01.2018, max: 31.12.2018
  quarter_year: /^.[q]([1-4])(\d{4})/,  // e.g. .q12018 > min: 01.01.2018, max: 31.03.2018
  date: /^\d{8}/ // e.g. 01012018 > 01.01.2018
}

export const specificDatePattern = (event) => {
  var value = event.target.value;
  var min, max, year, quarter;
  switch (true) {
    case DATE_PATTERN.year.test(value):
      year = parseInt(DATE_PATTERN.year.exec(value)[1]);
      min = moment([year, 0, 1]);
      max = moment([year, 11, 31]);
    break;
    case DATE_PATTERN.quarter_year.test(value):
      var match = DATE_PATTERN.quarter_year.exec(value)
      quarter = parseInt(match[1]);
      year = parseInt(match[2]);
      min = moment([year, 0, 1]).quarter(quarter);
      max = moment([year, 0, 1]).quarter(quarter + 1).subtract(1, "day");
    break;
    case DATE_PATTERN.date.test(value):
      min = moment(value, "DDMMYYYY");
    break;
    }

  return { minDate: min, maxDate: max }
}

export const parseDatePattern = (value, pattern) => {
  if (value && DATE_PATTERN.date.test(value))
    return moment(value, "DDMMYYYY")
}
