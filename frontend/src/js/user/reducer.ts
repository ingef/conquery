import { getType } from "typesafe-actions";

import type { GetMeResponseT } from "../api/types";
import type { Action } from "../app/actions";

import { loadMe } from "./actions";

export type UserStateT = {
  loading: boolean;
  error: string | null;
  me: GetMeResponseT | null;
};

const initialState: UserStateT = {
  loading: false,
  error: null,
  me: null,
};

const user = (state: UserStateT = initialState, action: Action): UserStateT => {
  switch (action.type) {
    case getType(loadMe.request):
      return {
        ...state,
        loading: true,
      };
    case getType(loadMe.failure):
      return {
        ...state,
        loading: false,
        error: action.payload.message || null,
      };
    case getType(loadMe.success):
      return {
        ...state,
        loading: false,
        me: action.payload.data,
      };
    default:
      return state;
  }
};

export default user;
