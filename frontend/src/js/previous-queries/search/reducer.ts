import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { updatePreviousQueriesSearch } from "./actions";

export type PreviousQueriesSearchStateT = string[];

const initialState: PreviousQueriesSearchStateT = [];

const previousQueriesSearch = (
  state: PreviousQueriesSearchStateT = initialState,
  action: Action,
): PreviousQueriesSearchStateT => {
  switch (action.type) {
    case getType(updatePreviousQueriesSearch):
      return action.payload;
    default:
      return state;
  }
};

export default previousQueriesSearch;
