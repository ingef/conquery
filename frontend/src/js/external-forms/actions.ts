import { ActionType, createAction } from "typesafe-actions";

import type { Forms } from "./config-types";

export type ExternalFormActions = ActionType<
  typeof setExternalForm | typeof loadFormsSuccess
>;

export const setExternalForm = createAction("forms/SET_EXTERNAL_FORM")<{
  form: string;
}>();

export const loadFormsSuccess = createAction("forms/LOAD_FORMS_SUCCESS")<{
  forms: Forms;
}>();
