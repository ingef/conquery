// @flow

import { createActionTypes } from './actionTypes';

export const createQueryNodeEditorActions = (type: string): Object => {
  const {
    SET_DETAILS_VIEW_ACTIVE,
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_NAME,
  } = createActionTypes(type);

  const setDetailsViewActive = () => ({type: SET_DETAILS_VIEW_ACTIVE});
  const setInputTableViewActive = (tableIdx) => ({type: SET_INPUT_TABLE_VIEW_ACTIVE, tableIdx});
  const setFocusedInput = (filterIdx) => ({type: SET_FOCUSED_INPUT, filterIdx});
  const toggleEditName = () => ({type: TOGGLE_EDIT_NAME});

  return {
    setDetailsViewActive,
    setInputTableViewActive,
    setFocusedInput,
    toggleEditName,
  };
};
