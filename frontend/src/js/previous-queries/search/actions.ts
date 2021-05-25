import { ActionType, createAction } from "typesafe-actions";

export type PreviousQueriesSearchActions = ActionType<
  typeof updatePreviousQueriesSearch
>;

export const updatePreviousQueriesSearch = createAction(
  "previous-queries/UPDATE_PREVIOUS_QUERIES_SEARCH",
)<string[]>();
