// @flow

import { createQueryNodeEditorActions } from "conquery/lib/js/query-node-editor/actions";

export const {
  setNode: setDescriptionFeaturesNode,
  clearNode: clearDescriptionFeaturesNode,
  loadFormFilterSuggestions: loadDescriptionFeaturesFormFilterSuggestions
} = createQueryNodeEditorActions("descriptionFeatures");
