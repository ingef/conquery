import { createActionTypes } from "./actionTypes";

export const createQueryNodeEditorActions = (type: string) => {
  const { SET_FOCUSED_INPUT, RESET } = createActionTypes(type);

  const setFocusedInput = (filterIdx: number) => ({
    type: SET_FOCUSED_INPUT,
    filterIdx,
  });
  const reset = () => ({ type: RESET });

  return {
    setFocusedInput,
    reset,
  };
};
