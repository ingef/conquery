import { useDispatch } from "react-redux";

import { useGetFrontendConfig } from "../api/api";
import { defaultError, defaultSuccess } from "../common/actions";

import {
  LOAD_CONFIG_START,
  LOAD_CONFIG_ERROR,
  LOAD_CONFIG_SUCCESS,
} from "./actionTypes";

export const loadConfigStart = () => ({ type: LOAD_CONFIG_START });
export const loadConfigError = (err: any) =>
  defaultError(LOAD_CONFIG_ERROR, err);
export const loadConfigSuccess = (res: any) =>
  defaultSuccess(LOAD_CONFIG_SUCCESS, res);

export const useLoadConfig = () => {
  const dispatch = useDispatch();
  const getFrontendConfig = useGetFrontendConfig();

  return async () => {
    dispatch(loadConfigStart());

    try {
      const result = await getFrontendConfig();
      dispatch(loadConfigSuccess(result));
    } catch (error) {
      dispatch(loadConfigError(error));
    }
  };
};
