import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import { setSelectedSecondaryId } from "./actions";

export type SelectedSecondaryIdStateT = string | null;

const reducer = (state: SelectedSecondaryIdStateT = null, action: Action) => {
  switch (action.type) {
    case getType(setSelectedSecondaryId):
      return action.payload.secondaryId;
    default:
      return state;
  }
};

export default reducer;
