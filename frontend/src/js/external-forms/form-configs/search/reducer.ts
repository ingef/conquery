import {
  SET_FORM_CONFIGS_SEARCH,
  ADD_TAG_TO_FORM_CONFIGS_SEARCH,
} from "./actionTypes";

export type FormConfigsSearchStateT = string[];

const initialState: FormConfigsSearchStateT = [];

const formConfigSearch = (
  state: FormConfigsSearchStateT = initialState,
  action: any,
): FormConfigsSearchStateT => {
  switch (action.type) {
    case SET_FORM_CONFIGS_SEARCH:
      return action.payload.values;
    case ADD_TAG_TO_FORM_CONFIGS_SEARCH:
      const { tag } = action.payload;

      // Only add tag if it doesn't exist
      return state.indexOf(tag) === -1 ? state.concat(tag) : state;
    default:
      return state;
  }
};

export default formConfigSearch;
