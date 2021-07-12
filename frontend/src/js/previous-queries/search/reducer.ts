import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { clearQueriesSearch, setQueriesSearch } from "./actions";

export interface QueriesSearchStateT {
  searchTerm: string | null;
  result: Record<string, number> | null;
  words: string[];
}

const initialState: QueriesSearchStateT = {
  searchTerm: null,
  result: null,
  words: [],
};

const previousQueriesSearch = (
  state: QueriesSearchStateT = initialState,
  action: Action,
): QueriesSearchStateT => {
  switch (action.type) {
    case getType(setQueriesSearch):
      return { ...state, ...action.payload };
    case getType(clearQueriesSearch):
      return initialState;
    default:
      return state;
  }
};

export default previousQueriesSearch;
