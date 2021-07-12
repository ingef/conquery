import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { setPreviousQueriesFilter } from "./actions";

export type PreviousQueriesFilterStateT = "all" | "own" | "shared" | "system";

const initialState: PreviousQueriesFilterStateT = "all";

const previousQueriesFilter = (
  state: PreviousQueriesFilterStateT = initialState,
  action: Action,
): PreviousQueriesFilterStateT => {
  switch (action.type) {
    case getType(setPreviousQueriesFilter):
      return action.payload;
    default:
      return state;
  }
};

export default previousQueriesFilter;
