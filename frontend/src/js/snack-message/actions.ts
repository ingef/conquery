import { ActionType, createAction } from "typesafe-actions";

import { SnackMessageStateT } from "./reducer";

export type SnackMessageActions = ActionType<
  typeof setMessage | typeof resetMessage
>;

export const setMessage = createAction("snack-message/SET")<{
  message: string | null;
  type: SnackMessageStateT["type"];
}>();

export const resetMessage = createAction("snack-message/RESET")();
