import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import { resetMessage, setMessage } from "./actions";

export type SnackMessageTypeT = "success" | "error" | null;
export interface SnackMessageStateT {
  message: string | null;
  notificationType: SnackMessageTypeT;
}

const initialState: SnackMessageStateT = {
  message: null,
  notificationType: null,
};

function reducer(
  state: SnackMessageStateT = initialState,
  action: Action,
): SnackMessageStateT {
  switch (action.type) {
    case getType(setMessage):
      return {
        ...state,
        message: action.payload.message,
        notificationType: action.payload.notificationType,
      };
    case getType(resetMessage):
      return initialState;
    default:
      return state;
  }
}

export default reducer;
