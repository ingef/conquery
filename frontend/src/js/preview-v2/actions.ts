import { ActionType, createAction } from "typesafe-actions";

export type PreviewActions = ActionType<
  typeof closePreview | typeof openPreview
>;

export const openPreview = createAction("preview/OPENk")();
export const closePreview = createAction("preview/CLOSE")();
