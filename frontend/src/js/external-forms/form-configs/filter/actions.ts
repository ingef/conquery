import { SET_FORM_CONFIGS_FILTER } from "./actionTypes";

export const setFormConfigsFilter = (filter: string) => ({
  type: SET_FORM_CONFIGS_FILTER,
  payload: { filter },
});
