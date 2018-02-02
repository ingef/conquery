import { type Dispatch }      from 'redux-thunk';

import api                    from '../api';
import {
  defaultError,
  defaultSuccess
} from "../common/actions";
import {
  LOAD_VERSION_START,
  LOAD_VERSION_SUCCESS,
  LOAD_VERSION_ERROR
} from './actionTypes';

export const loadVersionStart = () => ({ type: LOAD_VERSION_START });
export const loadVersionError = (err: any) => defaultError(LOAD_VERSION_ERROR, err);
export const loadVersionSuccess = (res: any) => defaultSuccess(LOAD_VERSION_SUCCESS, res);

export const loadVersion = () => {
  return (dispatch: Dispatch) => {
    dispatch(loadVersionStart());

    return api.getVersion()
      .then(
        r => {
          dispatch(loadVersionSuccess(r));
        },
        e => dispatch(loadVersionError(e)))
  }
};
