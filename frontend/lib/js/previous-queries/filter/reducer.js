import { SET_PREVIOUS_QUERIES_FILTER } from "./actionTypes";

const initialState = "all";

const previousQueriesFilter = (state = initialState, action) => {
  switch (action.type) {
    case SET_PREVIOUS_QUERIES_FILTER:
      return action.payload.filter;
    default:
      return state;
  }
};

export default previousQueriesFilter;
