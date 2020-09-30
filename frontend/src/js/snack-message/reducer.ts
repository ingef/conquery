import { SET_MESSAGE, RESET_MESSAGE } from "./actionTypes";

export interface SnackMessageStateT {
  messageKey: string | null;
}

const initialState: SnackMessageStateT = {
  messageKey: null,
};

export default (
  state: SnackMessageStateT = initialState,
  action: Object
): SnackMessageStateT => {
  switch (action.type) {
    case SET_MESSAGE:
      return { ...state, messageKey: action.payload.messageKey };
    case RESET_MESSAGE:
      return initialState;
    default:
      return state;
  }
};
