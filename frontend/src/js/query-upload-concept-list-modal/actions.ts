import { ActionType, createAction } from "typesafe-actions";

import { TreesT } from "../concept-trees/reducer";

export type QueryUploadConceptListModalActions = ActionType<
  | typeof openQueryUploadConceptListModal
  | typeof closeQueryUploadConceptListModal
  | typeof acceptQueryUploadConceptListModal
>;

export const openQueryUploadConceptListModal = createAction(
  "query-upload-concept-list-modal/OPEN",
)<{
  andIdx: number | null;
}>();

export const closeQueryUploadConceptListModal = createAction(
  "query-upload-concept-list-modal/CLOSE",
)();

export const acceptQueryUploadConceptListModal = createAction(
  "query-upload-concept-list-modal/ACCEPT",
)<{
  andIdx: number | null;
  label: string;
  rootConcepts: TreesT;
  resolvedConcepts: string[];
}>();
