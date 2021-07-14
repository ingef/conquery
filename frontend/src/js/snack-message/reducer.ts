import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import { resetMessage, setMessage } from "./actions";

export interface SnackMessageStateT {
  message: string | null;
}

const initialState: SnackMessageStateT = {
  message: null,
};

function reducer(
  state: SnackMessageStateT = initialState,
  action: Action,
): SnackMessageStateT {
  switch (action.type) {
    case getType(setMessage):
      return { ...state, message: action.payload.message };
    case getType(resetMessage):
      return initialState;
    default:
      return state;
  }
}

export default reducer;
