import { SET_EXTERNAL_FORM } from "./actionTypes";
import createQueryRunnerActions from "../query-runner/actions";

export const setExternalForm = (form: string) => ({
  type: SET_EXTERNAL_FORM,
  payload: { form }
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
  queryExternalFormsResult
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
  queryExternalFormsResult
};
