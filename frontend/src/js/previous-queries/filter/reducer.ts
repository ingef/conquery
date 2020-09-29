import { SET_PREVIOUS_QUERIES_FILTER } from "./actionTypes";

export type PreviousQueriesFilterStateT = string;

const initialState: PreviousQueriesFilterStateT = "all";

const previousQueriesFilter = (
  state: PreviousQueriesFilterStateT = initialState,
  action: Object
): PreviousQueriesFilterStateT => {
  switch (action.type) {
    case SET_PREVIOUS_QUERIES_FILTER:
      return action.payload.filter;
    default:
      return state;
  }
};

export default previousQueriesFilter;
