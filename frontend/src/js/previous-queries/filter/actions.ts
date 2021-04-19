import { SET_PREVIOUS_QUERIES_FILTER } from "./actionTypes";

export const setPreviousQueriesFilter = (filter) => ({
  type: SET_PREVIOUS_QUERIES_FILTER,
  payload: { filter },
});
