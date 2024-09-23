import { ActionType, createAction } from "typesafe-actions";

export type UploadConceptListModalActions = ActionType<
  typeof initUploadConceptListModal | typeof resetUploadConceptListModal
>;

export const initUploadConceptListModal = createAction(
  "upload-concept-list-modal/INIT",
)<{
  rows: string[];
  filename: string;
}>();

export const resetUploadConceptListModal = createAction(
  "upload-concept-list-modal/RESET",
)();
