import { ActionType, createAction } from "typesafe-actions";

export type SnackMessageActions = ActionType<
  typeof setMessage | typeof resetMessage
>;

export const setMessage = createAction("snack-message/SET")<{
  message: string | null;
}>();

export const resetMessage = createAction("snack-message/RESET")();
