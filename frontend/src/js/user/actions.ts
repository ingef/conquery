import { useGetMe } from "../api/api";
import { useDispatch } from "react-redux";
import { defaultError, defaultSuccess } from "../common/actions";

import { LOAD_ME_START, LOAD_ME_ERROR, LOAD_ME_SUCCESS } from "./actionTypes";

export const loadMeStart = () => ({ type: LOAD_ME_START });
export const loadMeError = (err: any) => defaultError(LOAD_ME_ERROR, err);
export const loadMeSuccess = (res: any) => defaultSuccess(LOAD_ME_SUCCESS, res);

export const useLoadMe = () => {
  const dispatch = useDispatch();
  const getMe = useGetMe();

  return () => {
    dispatch(loadMeStart());

    return getMe().then(
      (r) => dispatch(loadMeSuccess(r)),
      (e) => dispatch(loadMeError(e))
    );
  };
};
