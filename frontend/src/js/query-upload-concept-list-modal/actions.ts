import { type ActionType, createAction } from "typesafe-actions";

import type { SelectOptionT } from "../api/types";
import type { TreesT } from "../concept-trees/reducer";

export type QueryUploadConceptListModalActions = ActionType<
  typeof acceptUploadedConceptsOrFilter
>;

export const acceptUploadedConceptsOrFilter = createAction(
  "query-upload-concept-list-modal/ACCEPT_CONCEPTS_OR_FILTER",
)<{
  andIdx?: number;
  label: string;
  rootConcepts: TreesT;
  resolvedConcepts: string[];
  resolvedFilter?: {
    tableId: string;
    filterId: string;
    value: SelectOptionT[];
  };
}>();
