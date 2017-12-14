// @flow

import moment from "moment";

export const formatDate = (dateString: String) => {
  return moment(dateString).format(moment.localeData().longDateFormat("L"))
};