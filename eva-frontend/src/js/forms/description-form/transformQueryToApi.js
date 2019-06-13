// @flow

import { T } from "conquery/lib/js/localization";
import { transformElementsToApi } from "conquery/lib/js/api/apiHelper";

export const transformDescriptionFormQueryToApi = descriptionForm => {
  const timeMode = descriptionForm.timeMode;

  if (timeMode === "ABSOLUTE")
    return {
      type: "DESCRIPTION_FORM_ABSOLUTE_TIME",
      title: descriptionForm.title || T.translate("common.title"),
      description:
        descriptionForm.description || T.translate("common.description"),
      dateRange: {
        min: descriptionForm.dateRange.min,
        max: descriptionForm.dateRange.max
      },
      queryGroup: descriptionForm.queryGroup.id,
      features: transformDescriptionElementGroupsToApi(
        descriptionForm.features
      ),
      resolution: descriptionForm.resolution
    };

  if (timeMode === "RELATIVE")
    return {
      type: "DESCRIPTION_FORM_RELATIVE_TIME",
      title: descriptionForm.title || T.translate("common.title"),
      description:
        descriptionForm.description || T.translate("common.description"),
      features: transformDescriptionElementGroupsToApi(
        descriptionForm.features
      ),
      queryGroup: {
        id: descriptionForm.queryGroup.id,
        timestamp: descriptionForm.timestamp
      },
      outcomes: transformDescriptionElementGroupsToApi(
        descriptionForm.outcomes
      ),
      timeCountBefore: descriptionForm.timeCountBefore,
      timeCountAfter: descriptionForm.timeCountAfter,
      indexDate: descriptionForm.indexDate,
      resolution: descriptionForm.resolution
    };
};

const transformDescriptionElementGroupsToApi = elementGroups =>
  elementGroups.map(elements => ({
    type: "OR",
    children: transformElementsToApi(elements.concepts)
  }));
