import { useCallback } from "react";
import { useDispatch } from "react-redux";
import { ActionType, createAsyncAction } from "typesafe-actions";

import { useGetMe } from "../api/api";
import type { GetMeResponseT } from "../api/types";
import { ErrorObject, errorPayload, successPayload } from "../common/actions";

export type UserActions = ActionType<typeof loadMe>;

export const loadMe = createAsyncAction(
  "user/LOAD_ME_START",
  "user/LOAD_ME_SUCCESS",
  "user/LOAD_ME_ERROR",
)<void, { data: GetMeResponseT }, ErrorObject>();

export const useLoadMe = () => {
  const dispatch = useDispatch();
  const getMe = useGetMe();

  return useCallback(async () => {
    dispatch(loadMe.request());

    try {
      const response = await getMe();
      dispatch(loadMe.success(successPayload(response, {})));
    } catch (error) {
      dispatch(loadMe.failure(errorPayload(error as Error, {})));
    }
  }, [dispatch, getMe]);
};
