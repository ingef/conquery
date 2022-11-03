import { useCallback } from "react";
import { useDispatch } from "react-redux";
import { ActionType, createAsyncAction } from "typesafe-actions";

import { useGetFrontendConfig } from "../api/api";
import { GetFrontendConfigResponseT } from "../api/types";
import { ErrorObject, errorPayload, successPayload } from "../common/actions";

export type StartupActions = ActionType<typeof loadConfig>;

export const loadConfig = createAsyncAction(
  "startup/LOAD_CONFIG_START",
  "startup/LOAD_CONFIG_SUCCESS",
  "startup/LOAD_CONFIG_ERROR",
)<undefined, { data: GetFrontendConfigResponseT }, ErrorObject>();

export const useLoadConfig = () => {
  const dispatch = useDispatch();
  const getFrontendConfig = useGetFrontendConfig();

  return useCallback(async () => {
    dispatch(loadConfig.request());

    try {
      const result = await getFrontendConfig();
      dispatch(loadConfig.success(successPayload(result, {})));
    } catch (error) {
      dispatch(loadConfig.failure(errorPayload(error as Error, {})));
    }
  }, [dispatch, getFrontendConfig]);
};
