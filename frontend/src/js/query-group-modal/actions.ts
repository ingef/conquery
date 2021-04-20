import type { DateRangeT } from "../api/types";

import {
  QUERY_GROUP_MODAL_SET_DATE,
  QUERY_GROUP_MODAL_RESET_ALL_DATES,
} from "./actionTypes";

export const queryGroupModalSetDate = (andIdx: number, date: DateRangeT) => ({
  type: QUERY_GROUP_MODAL_SET_DATE,
  payload: { andIdx, date },
});

export const queryGroupModalResetAllDates = (andIdx: number) => ({
  type: QUERY_GROUP_MODAL_RESET_ALL_DATES,
  payload: { andIdx },
});
