import { SET_MESSAGE, RESET_MESSAGE } from "./actionTypes";

export type StateType = {
  messageKey: string | null;
};

const initialState: StateType = {
  messageKey: null
};

export default (state: StateType = initialState, action: Object): StateType => {
  switch (action.type) {
    case SET_MESSAGE:
      return { ...state, messageKey: action.payload.messageKey };
    case RESET_MESSAGE:
      return initialState;
    default:
      return state;
  }
};
