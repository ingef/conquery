import type { Dispatch } from "redux-thunk";

import api from "../api";
import { defaultError, defaultSuccess } from "../common/actions";

import { LOAD_ME_START, LOAD_ME_ERROR, LOAD_ME_SUCCESS } from "./actionTypes";

export const startup = () => loadMe();

export const loadMeStart = () => ({ type: LOAD_ME_START });
export const loadMeError = (err: any) => defaultError(LOAD_ME_ERROR, err);
export const loadMeSuccess = (res: any) => defaultSuccess(LOAD_ME_SUCCESS, res);

export const loadMe = () => {
  return (dispatch: Dispatch) => {
    dispatch(loadMeStart());

    return api.getMe().then(
      r => dispatch(loadMeSuccess(r)),
      e => dispatch(loadMeError(e))
    );
  };
};
