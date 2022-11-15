import { ActionType, createAction } from "typesafe-actions";

import { SelectOptionT } from "../api/types";
import { TreesT } from "../concept-trees/reducer";

export type QueryUploadConceptListModalActions = ActionType<
  typeof acceptUploadedConcepts | typeof acceptUploadedFilters
>;

export const acceptUploadedConcepts = createAction(
  "query-upload-concept-list-modal/ACCEPT_CONCEPTS",
)<{
  andIdx?: number;
  label: string;
  rootConcepts: TreesT;
  resolvedConcepts: string[];
}>();

export const acceptUploadedFilters = createAction(
  "query-upload-concept-list-modal/ACCEPT_FILTERS",
)<{
  andIdx?: number;
  label: string;
  rootConcepts: TreesT;
  resolvedConcepts: string[];
  tableId: string;
  filterId: string;
  resolvedFilterValue: SelectOptionT[];
}>();
