import { GetMeResponseT } from "js/api/types";
import { useDispatch } from "react-redux";
import { ActionType, createAsyncAction } from "typesafe-actions";

import { useGetMe } from "../api/api";
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

  return () => {
    dispatch(loadMe.request());

    return getMe().then(
      (r) => dispatch(loadMe.success(successPayload(r, {}))),
      (e) => dispatch(loadMe.failure(errorPayload(e, {}))),
    );
  };
};
