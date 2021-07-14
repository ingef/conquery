import { ActionType, createAction } from "typesafe-actions";

import type { PreviousQueriesFilterStateT } from "./reducer";

export type PreviousQueriesFilterActions = ActionType<
  typeof setPreviousQueriesFilter
>;

export const setPreviousQueriesFilter = createAction(
  "previous-queries/SET_FILTER",
)<PreviousQueriesFilterStateT>();
