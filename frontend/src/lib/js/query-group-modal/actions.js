import {
  QUERY_GROUP_MODAL_SET_NODE,
  QUERY_GROUP_MODAL_CLEAR_NODE,
  QUERY_GROUP_MODAL_SET_MIN_DATE,
  QUERY_GROUP_MODAL_SET_MAX_DATE,
  QUERY_GROUP_MODAL_RESET_ALL_DATES,
  QUERY_GROUP_MODAL_SET_TOUCHED
} from "./actionTypes";

export const queryGroupModalSetNode = andIdx => ({
  type: QUERY_GROUP_MODAL_SET_NODE,
  payload: { andIdx }
});

export const queryGroupModalClearNode = () => ({
  type: QUERY_GROUP_MODAL_CLEAR_NODE
});

export const queryGroupModalSetMinDate = (andIdx, date) => ({
  type: QUERY_GROUP_MODAL_SET_MIN_DATE,
  payload: { andIdx, date }
});

export const queryGroupModalSetMaxDate = (andIdx, date) => ({
  type: QUERY_GROUP_MODAL_SET_MAX_DATE,
  payload: { andIdx, date }
});

export const queryGroupModalResetAllDates = andIdx => ({
  type: QUERY_GROUP_MODAL_RESET_ALL_DATES,
  payload: { andIdx }
});

export const queryGroupModalSetTouched = (andIdx, field) => ({
  type: QUERY_GROUP_MODAL_SET_TOUCHED,
  payload: { andIdx, field }
});
