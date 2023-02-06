import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import { resetMessage, setMessage } from "./actions";

export enum SnackMessageType {
  ERROR = "error",
  SUCCESS = "success",
  DEFAULT = "default",
}
export interface SnackMessageStateT {
  message: string | null;
  type: SnackMessageType;
}

const initialState: SnackMessageStateT = {
  message: null,
  type: SnackMessageType.DEFAULT,
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
        type: action.payload.type,
      };
    case getType(resetMessage):
      return initialState;
    default:
      return state;
  }
}

export default reducer;
