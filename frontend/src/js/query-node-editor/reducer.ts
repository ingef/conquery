import { createActionTypes } from "./actionTypes";

export interface QueryNodeEditorStateT {
  selectedInputTableIdx: number | null;
  selectedInput: number | null; // It's a filter index => TODO: Refactor/rename
}

export const createQueryNodeEditorReducer = (type: string) => {
  const initialState: QueryNodeEditorStateT = {
    selectedInputTableIdx: null,
    selectedInput: null,
  };

  const {
    SET_INPUT_TABLE_VIEW_ACTIVE,
    SET_FOCUSED_INPUT,
    RESET,
  } = createActionTypes(type);

  return (
    state: QueryNodeEditorStateT = initialState,
    action: any,
  ): QueryNodeEditorStateT => {
    switch (action.type) {
      case SET_INPUT_TABLE_VIEW_ACTIVE:
        return {
          ...state,
          selectedInputTableIdx: action.tableIdx,
          selectedInput: null,
        };
      case SET_FOCUSED_INPUT:
        return {
          ...state,
          selectedInput: action.filterIdx,
        };
      case RESET:
        return initialState;
      default:
        return state;
    }
  };
};
