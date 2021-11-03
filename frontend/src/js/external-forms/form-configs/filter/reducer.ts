import { SET_FORM_CONFIGS_FILTER } from "./actionTypes";

export type FormConfigsFilterStateT = string;

const initialState: FormConfigsFilterStateT = "own";

const formConfigsFilter = (
  state: FormConfigsFilterStateT = initialState,
  action: any,
): FormConfigsFilterStateT => {
  switch (action.type) {
    case SET_FORM_CONFIGS_FILTER:
      return action.payload.filter;
    default:
      return state;
  }
};

export default formConfigsFilter;
