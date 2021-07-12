import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { setPreviousQueriesSearch } from "./actions";

export interface PreviousQueriesSearchStateT {
  query: string | null;
  result: Record<string, number> | null;
}

const initialState: PreviousQueriesSearchStateT = {
  query: null,
  result: null,
};

const previousQueriesSearch = (
  state: PreviousQueriesSearchStateT = initialState,
  action: Action,
): PreviousQueriesSearchStateT => {
  switch (action.type) {
    case getType(setPreviousQueriesSearch):
      return { ...state, query: action.payload };
    default:
      return state;
  }
};

export default previousQueriesSearch;
