import { createActionTypes } from "./actionTypes";

export interface QueryNodeEditorStateT {
  selectedInput: number | null; // It's a filter index => TODO: Refactor/rename
}

export const createQueryNodeEditorReducer = (type: string) => {
  const initialState: QueryNodeEditorStateT = {
    selectedInput: null,
  };

  const { SET_FOCUSED_INPUT, RESET } = createActionTypes(type);

  return (
    state: QueryNodeEditorStateT = initialState,
    action: any,
  ): QueryNodeEditorStateT => {
    switch (action.type) {
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
