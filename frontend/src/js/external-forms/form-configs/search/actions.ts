import {
  SET_FORM_CONFIGS_SEARCH,
  ADD_TAG_TO_FORM_CONFIGS_SEARCH,
} from "./actionTypes";

export const setFormConfigsSearch = (values: string[]) => ({
  type: SET_FORM_CONFIGS_SEARCH,
  payload: { values },
});

export const addTagToFormConfigsSearch = (tag: string) => ({
  type: ADD_TAG_TO_FORM_CONFIGS_SEARCH,
  payload: { tag },
});
