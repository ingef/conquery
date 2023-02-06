import { ActionType, createAction } from "typesafe-actions";

import { SnackMessageType } from "./reducer";

export type SnackMessageActions = ActionType<
  typeof setMessage | typeof resetMessage
>;

export const setMessage = createAction("snack-message/SET")<{
  message: string | null;
  type: SnackMessageType;
}>();

export const resetMessage = createAction("snack-message/RESET")();
