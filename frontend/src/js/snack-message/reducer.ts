import { SET_MESSAGE, RESET_MESSAGE } from "./actionTypes";

export interface SnackMessageStateT {
  message: string | null;
}

const initialState: SnackMessageStateT = {
  message: null,
};

export default (
  state: SnackMessageStateT = initialState,
  action: Object
): SnackMessageStateT => {
  switch (action.type) {
    case SET_MESSAGE:
      return { ...state, message: action.payload.message };
    case RESET_MESSAGE:
      return initialState;
    default:
      return state;
  }
};
