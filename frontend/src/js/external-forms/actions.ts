import { ActionType, createAction } from "typesafe-actions";

export type ExternalFormActions = ActionType<typeof setExternalForm>;

export const setExternalForm = createAction("form/SET_EXTERNAL_FORM")<{
  form: string;
}>();
