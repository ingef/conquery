import { ActionType, createAction } from "typesafe-actions";

export type PreviousQueriesFilterActions = ActionType<
  typeof setPreviousQueriesFilter
>;

export const setPreviousQueriesFilter = createAction(
  "previous-queries/SET_FILTER",
)<string>();
