import {
  UPDATE_PREVIOUS_QUERIES_SEARCH,
  ADD_TAG_TO_PREVIOUS_QUERIES_SEARCH
} from "./actionTypes";

type StateT = string[];

const initialState: StateT = [];

const previousQueriesSearch = (state: StateT = initialState, action) => {
  switch (action.type) {
    case UPDATE_PREVIOUS_QUERIES_SEARCH:
      return action.payload.values;
    case ADD_TAG_TO_PREVIOUS_QUERIES_SEARCH:
      const { tag } = action.payload;

      // Only add tag if it doesn't exist
      return state.indexOf(tag) === -1 ? state.concat(tag) : state;
    default:
      return state;
  }
};

export default previousQueriesSearch;
