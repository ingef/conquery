// @flow

import moment from "moment";

export const formatDate = (dateString: String) => {
  return moment(dateString).format(moment.localeData().longDateFormat("L"))
};

export const duration = (value, units: string, format: string) => {
  return moment.duration(value, units).format(format, {trim: false});
}
