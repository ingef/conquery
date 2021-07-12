import { ActionType, createAction } from "typesafe-actions";

export type PreviousQueriesSearchActions = ActionType<
  typeof setPreviousQueriesSearch
>;

export const setPreviousQueriesSearch = createAction(
  "previous-queries/SET_PREVIOUS_QUERIES_SEARCH",
)<string | null>();
