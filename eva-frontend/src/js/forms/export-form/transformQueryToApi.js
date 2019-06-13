// @flow

import { transformElementsToApi } from "conquery/lib/js/api/apiHelper";

export const transformExportFormQueryToApi = exportForm => {
  const timeMode = exportForm.timeMode;

  if (timeMode === "ABSOLUTE")
    return {
      type: "EXPORT_FORM_ABSOLUTE_TIME",
      dateRange: {
        min: exportForm.dateRange.min,
        max: exportForm.dateRange.max
      },
      queryGroup: exportForm.queryGroup.id,
      features: transformExportElementGroupsToApi(exportForm.outcomes)
    };

  if (timeMode === "RELATIVE")
    return {
      type: "EXPORT_FORM",
      indexDate: exportForm.indexDate,
      timeCountBefore: exportForm.timeCountBefore,
      timeCountAfter: exportForm.timeCountAfter,
      timeUnit: exportForm.timeUnit,
      features: transformExportElementGroupsToApi(exportForm.features),
      outcomes: transformExportElementGroupsToApi(exportForm.outcomes),
      queryGroup: {
        id: exportForm.queryGroup.id,
        timestamp: exportForm.timestamp
      }
    };
};

const transformExportElementGroupsToApi = elementGroups =>
  elementGroups.map(elements => ({
    type: "OR",
    children: transformElementsToApi(elements.concepts)
  }));
