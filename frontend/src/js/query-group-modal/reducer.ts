import {
  QUERY_GROUP_MODAL_SET_NODE,
  QUERY_GROUP_MODAL_CLEAR_NODE,
} from "./actionTypes";

const initialState = {};

const queryGroupModal = (state = initialState, action) => {
  switch (action.type) {
    case QUERY_GROUP_MODAL_SET_NODE:
      return {
        ...state,
        andIdx: action.payload.andIdx,
      };
    case QUERY_GROUP_MODAL_CLEAR_NODE:
      return { ...state, andIdx: undefined };
    default:
      return state;
  }
};

export default queryGroupModal;
