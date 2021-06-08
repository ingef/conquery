import { createActionTypes } from "./actionTypes";

export const createQueryNodeEditorActions = (type: string) => {
  const {
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_LABEL,
    RESET,
  } = createActionTypes(type);

  const setInputTableViewActive = (tableIdx: number) => ({
    type: SET_INPUT_TABLE_VIEW_ACTIVE,
    tableIdx,
  });
  const setFocusedInput = (filterIdx: number) => ({
    type: SET_FOCUSED_INPUT,
    filterIdx,
  });
  const toggleEditLabel = () => ({ type: TOGGLE_EDIT_LABEL });
  const reset = () => ({ type: RESET });

  return {
    setInputTableViewActive,
    setFocusedInput,
    toggleEditLabel,
    reset,
  };
};
