// @flow

import { T } from "conquery/lib/js/localization";
import { transformElementsToApi } from "conquery/lib/js/api/apiHelper";

export const transformMapFormQueryToApi = form => {
  return {
    ...form,
    type: "MAP_FORM",
    title: form.title || T.translate("common.title"),
    description: form.description || T.translate("common.description"),
    dateRange: {
      min: form.dateRange.min,
      max: form.dateRange.max
    },
    queryGroup: form.queryGroup.id,
    features: transformMapElementGroupsToApi(form.features),
    // json deserialization in backend adds 'is' prefix implicitly
    isRelative: undefined,
    relative: form.isRelative
  };
};

const transformMapElementGroupsToApi = elementGroups =>
  elementGroups.map(elements => ({
    type: "OR",
    children: transformElementsToApi(elements.concepts)
  }));
