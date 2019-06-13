// @flow

import { createQueryNodeEditorActions } from "conquery/lib/js/query-node-editor";

export const {
  setNode: setMapFeaturesNode,
  clearNode: clearMapFeaturesNode,
  loadFormFilterSuggestions: loadMapFeaturesFormFilterSuggestions
} = createQueryNodeEditorActions("mapFeatures");
