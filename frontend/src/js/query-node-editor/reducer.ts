import { createActionTypes } from "./actionTypes";

export const createQueryNodeEditorReducer = (type: string) => {
  const initialState = {
    detailsViewActive: true,
    selectedInputTableIdx: 0,
    selectedInput: null,
    editingLabel: false
  };

  const {
    SET_DETAILS_VIEW_ACTIVE,
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_LABEL,
    RESET
  } = createActionTypes(type);

  return (state = initialState, action) => {
    switch (action.type) {
      case SET_DETAILS_VIEW_ACTIVE:
        return {
          ...state,
          detailsViewActive: true
        };
      case SET_INPUT_TABLE_VIEW_ACTIVE:
        return {
          ...state,
          detailsViewActive: false,
          selectedInputTableIdx: action.tableIdx,
          selectedInput: null
        };
      case SET_FOCUSED_INPUT:
        return {
          ...state,
          selectedInput: action.filterIdx
        };
      case TOGGLE_EDIT_LABEL:
        return {
          ...state,
          editingLabel: !state.editingLabel
        };
      case RESET:
        return initialState;
      default:
        return state;
    }
  };
};
