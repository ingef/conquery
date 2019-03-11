import {
  DELETE_PREVIOUS_QUERY_MODAL_OPEN,
  DELETE_PREVIOUS_QUERY_MODAL_CLOSE
} from "./actionTypes";

export const deletePreviousQueryModalOpen = queryId => ({
  type: DELETE_PREVIOUS_QUERY_MODAL_OPEN,
  payload: { queryId }
});

export const deletePreviousQueryModalClose = () => ({
  type: DELETE_PREVIOUS_QUERY_MODAL_CLOSE
});
