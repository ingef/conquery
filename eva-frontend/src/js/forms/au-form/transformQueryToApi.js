// @flow

import { T } from "conquery/lib/js/localization";
import { transformElementsToApi } from "conquery/lib/js/api/apiHelper";

export const transformAUFormQueryToApi = form => {
  return {
    ...form,
    type: "AU_FORM",
    title: form.title || T.translate("common.title"),
    description: form.description || T.translate("common.description"),
    dateRange: {
      min: form.dateRange.min,
      max: form.dateRange.max
    },
    queryGroup: form.queryGroup.id,
    baseCondition: transformAUElementGroupsToApi(form.baseCondition)
  };
};

const transformAUElementGroupsToApi = elementGroups =>
  elementGroups.map(elements => ({
    type: "OR",
    children: transformElementsToApi(elements.concepts)
  }));
