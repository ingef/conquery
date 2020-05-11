import { SET_EXTERNAL_FORM, LOAD_EXTERNAL_FORM_VALUES } from "./actionTypes";
import createQueryRunnerActions from "../query-runner/actions";

export const setExternalForm = (form: string) => ({
  type: SET_EXTERNAL_FORM,
  payload: { form },
});

export const loadExternalFormValues = (formType: string, values: any) => ({
  type: LOAD_EXTERNAL_FORM_VALUES,
  payload: { formType, values },
});

const {
  startExternalFormsQueryStart,
  startExternalFormsQueryError,
  startExternalFormsQuerySuccess,
  startExternalFormsQuery,
  stopExternalFormsQueryStart,
  stopExternalFormsQueryError,
  stopExternalFormsQuerySuccess,
  stopExternalFormsQuery,
  queryExternalFormsResultStart,
  queryExternalFormsResultStop,
  queryExternalFormsResultError,
  queryExternalFormsResultSuccess,
  queryExternalFormsResult,
} = createQueryRunnerActions("externalForms", true);

export {
  startExternalFormsQueryStart,
  startExternalFormsQueryError,
  startExternalFormsQuerySuccess,
  startExternalFormsQuery,
  stopExternalFormsQueryStart,
  stopExternalFormsQueryError,
  stopExternalFormsQuerySuccess,
  stopExternalFormsQuery,
  queryExternalFormsResultStart,
  queryExternalFormsResultStop,
  queryExternalFormsResultError,
  queryExternalFormsResultSuccess,
  queryExternalFormsResult,
};
