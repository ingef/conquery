import type { Dispatch } from "redux-thunk";

import api from "../api";
import { loadDatasets } from "../dataset/actions";
import { defaultError, defaultSuccess } from "../common/actions";

import {
  LOAD_CONFIG_START,
  LOAD_CONFIG_ERROR,
  LOAD_CONFIG_SUCCESS
} from "./actionTypes";

export const startup = () => loadDatasets();

export const loadConfigStart = () => ({ type: LOAD_CONFIG_START });
export const loadConfigError = (err: any) =>
  defaultError(LOAD_CONFIG_ERROR, err);
export const loadConfigSuccess = (res: any) =>
  defaultSuccess(LOAD_CONFIG_SUCCESS, res);

export const loadConfig = () => {
  return (dispatch: Dispatch) => {
    dispatch(loadConfigStart());

    return api.getFrontendConfig().then(
      r => dispatch(loadConfigSuccess(r)),
      e => dispatch(loadConfigError(e))
    );
  };
};
