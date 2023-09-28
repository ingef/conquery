import { ActionType, createAction } from "typesafe-actions";

export type PreviewActions = ActionType<
  typeof closePreview | typeof openPreview
>;

export const openPreview = createAction("preview/OPEN")();
export const closePreview = createAction("preview/CLOSE")();
