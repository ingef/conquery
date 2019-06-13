// @flow

import { createQueryNodeEditorActions } from "conquery/lib/js/query-node-editor/actions";

export const {
  loadFormFilterSuggestions: loadPsmFeaturesFormFilterSuggestions
} = createQueryNodeEditorActions("psmFeatures");

export const {
  loadFormFilterSuggestions: loadPsmOutcomesFormFilterSuggestions
} = createQueryNodeEditorActions("psmOutcomes");
