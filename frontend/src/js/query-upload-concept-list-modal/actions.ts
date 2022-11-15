import { ActionType, createAction } from "typesafe-actions";

import { TreesT } from "../concept-trees/reducer";

export type QueryUploadConceptListModalActions = ActionType<
  typeof acceptQueryUploadConceptListModal
>;

export const acceptQueryUploadConceptListModal = createAction(
  "query-upload-concept-list-modal/ACCEPT",
)<{
  andIdx?: number;
  label: string;
  rootConcepts: TreesT;
  resolvedConcepts: string[];
}>();
