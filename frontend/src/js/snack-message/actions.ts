import { ActionType, createAction } from "typesafe-actions";

import { SnackMessageTypeT } from "./reducer";

export type SnackMessageActions = ActionType<
  typeof setMessage | typeof resetMessage
>;

export const setMessage = createAction("snack-message/SET")<{
  message: string | null;
  notificationType: SnackMessageTypeT;
}>();

export const resetMessage = createAction("snack-message/RESET")();
