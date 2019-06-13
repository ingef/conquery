// @flow

import { createQueryNodeEditorActions } from "conquery/lib/js/query-node-editor/actions";

export const {
  setNode: setExportFeaturesNode,
  clearNode: clearExportFeaturesNode,
  loadFormFilterSuggestions: loadExportFeaturesFormFilterSuggestions
} = createQueryNodeEditorActions("exportFeatures");

export const {
  setNode: setExportOutcomesNode,
  clearNode: clearExportOutcomesNode,
  loadFormFilterSuggestions: loadExportOutcomesFormFilterSuggestions
} = createQueryNodeEditorActions("exportOutcomes");
