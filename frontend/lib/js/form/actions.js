// @flow

import { createQueryRunnerActions } from '../query-runner';
import { SET_FORM }      from './actionTypes';

export const setForm = (form: string) => ({
  type: SET_FORM,
  payload: { form }
});


const {
  startFormQueryStart,
  startFormQueryError,
  startFormQuerySuccess,
  startFormQuery,
  stopFormQueryStart,
  stopFormQueryError,
  stopFormQuerySuccess,
  stopFormQuery,
  queryFormResultStart,
  queryFormResultStop,
  queryFormResultError,
  queryFormResultSuccess,
  queryFormResult,
} = createQueryRunnerActions('form', true);

export {
  startFormQueryStart,
  startFormQueryError,
  startFormQuerySuccess,
  startFormQuery,
  stopFormQueryStart,
  stopFormQueryError,
  stopFormQuerySuccess,
  stopFormQuery,
  queryFormResultStart,
  queryFormResultStop,
  queryFormResultError,
  queryFormResultSuccess,
  queryFormResult,
};
