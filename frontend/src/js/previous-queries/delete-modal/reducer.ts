import {
  DELETE_PREVIOUS_QUERY_MODAL_OPEN,
  DELETE_PREVIOUS_QUERY_MODAL_CLOSE
} from "./actionTypes";

const initialState = {};

const deletePreviousQueryModal = (state = initialState, action) => {
  switch (action.type) {
    case DELETE_PREVIOUS_QUERY_MODAL_OPEN:
      return { ...state, queryId: action.payload.queryId };
    case DELETE_PREVIOUS_QUERY_MODAL_CLOSE:
      return { ...state, queryId: null };
    default:
      return state;
  }
};

export default deletePreviousQueryModal;
