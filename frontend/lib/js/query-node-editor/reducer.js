import { createActionTypes }     from './actionTypes';

export const createQueryNodeEditorReducer = (type: string) => {
  const initialState = {
    detailsViewActive: true,
    selectedInputTableIdx: 0,
    selectedInput: null,
    editingName: false
   };

  const {
    SET_DETAILS_VIEW_ACTIVE,
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    TOGGLE_EDIT_NAME,
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
          selectedInput: null,
        }
      case SET_FOCUSED_INPUT:
        return {
          ...state,
          selectedInput: action.filterIdx
        }
      case TOGGLE_EDIT_NAME:
        return {
          ...state,
          editingName: !state.editingName
        };
      default:
        return state;
    }
  };
}
