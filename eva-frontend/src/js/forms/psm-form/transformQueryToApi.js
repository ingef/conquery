// @flow

import { T } from "conquery/lib/js/localization";
import { transformElementsToApi } from "conquery/lib/js/api/apiHelper";

export const transformElementGroupsToApi = elementGroups =>
  elementGroups.map(elements => ({
    matchingType: elements.matchingType,
    type: "MATCHED",
    children: transformElementsToApi(elements.concepts)
  }));

export const transformPSMFormQueryToApi = psmForm => {
  const {
    featureGroupTimestamp,
    controlGroupTimestamp,
    featureGroupDataset,
    controlGroupDataset,
    ...psmFormRest
  } = psmForm;

  return {
    ...psmFormRest,
    features: transformElementGroupsToApi(psmForm.features),
    outcomes: transformElementGroupsToApi(psmForm.outcomes),
    featureGroup: {
      id: psmForm.featureGroup.id,
      timestamp: featureGroupTimestamp,
      datasetId: featureGroupDataset
    },
    controlGroup: {
      id: psmForm.controlGroup.id,
      timestamp: controlGroupTimestamp,
      datasetId: controlGroupDataset
    },
    title: psmForm.title || T.translate("common.title"),
    description: psmForm.description || T.translate("common.description")
  };
};
