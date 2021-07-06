import { ActionType, createAction } from "typesafe-actions";

import type { DateRangeT } from "../api/types";

export type QueryGroupModalActions = ActionType<
  typeof queryGroupModalSetDate | typeof queryGroupModalResetAllDates
>;

export const queryGroupModalSetDate = createAction(
  "query-group-modal/SET_DATE",
)<{ andIdx: number; date: DateRangeT }>();
export const queryGroupModalResetAllDates = createAction(
  "query-group-modal/RESET_ALL_DATES",
)<{ andIdx: number }>();
