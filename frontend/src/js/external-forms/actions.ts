import { SET_EXTERNAL_FORM, LOAD_EXTERNAL_FORM_VALUES } from "./actionTypes";

export const setExternalForm = (form: string) => ({
  type: SET_EXTERNAL_FORM,
  payload: { form },
});

export const loadExternalFormValues = (formType: string, values: any) => ({
  type: LOAD_EXTERNAL_FORM_VALUES,
  payload: { formType, values },
});
