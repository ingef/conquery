import { createActionTypes } from "./actionTypes";

export const createQueryNodeEditorActions = (type: string): Object => {
  const {
    SET_DETAILS_VIEW_ACTIVE,
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_LABEL,
    RESET
  } = createActionTypes(type);

  const setDetailsViewActive = () => ({ type: SET_DETAILS_VIEW_ACTIVE });
  const setInputTableViewActive = tableIdx => ({
    type: SET_INPUT_TABLE_VIEW_ACTIVE,
    tableIdx
  });
  const setFocusedInput = filterIdx => ({ type: SET_FOCUSED_INPUT, filterIdx });
  const toggleEditLabel = () => ({ type: TOGGLE_EDIT_LABEL });
  const reset = () => ({ type: RESET });

  return {
    setDetailsViewActive,
    setInputTableViewActive,
    setFocusedInput,
    toggleEditLabel,
    reset
  };
};
