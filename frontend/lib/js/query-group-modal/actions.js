import {
  QUERY_GROUP_MODAL_SET_NODE,
  QUERY_GROUP_MODAL_CLEAR_NODE,
  QUERY_GROUP_MODAL_SET_DATE,
  QUERY_GROUP_MODAL_RESET_ALL_DATES
} from "./actionTypes";

export const queryGroupModalSetNode = andIdx => ({
  type: QUERY_GROUP_MODAL_SET_NODE,
  payload: { andIdx }
});

export const queryGroupModalClearNode = () => ({
  type: QUERY_GROUP_MODAL_CLEAR_NODE
});

export const queryGroupModalSetDate = (andIdx, date) => ({
  type: QUERY_GROUP_MODAL_SET_DATE,
  payload: { andIdx, date }
});

export const queryGroupModalResetAllDates = andIdx => ({
  type: QUERY_GROUP_MODAL_RESET_ALL_DATES,
  payload: { andIdx }
});
