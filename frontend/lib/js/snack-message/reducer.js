// @flow

import { SET_MESSAGE } from "./actionTypes";

export type StateType = {
  messageKey: ?string
};

const initialState: StateType = {
  messageKey: null
};

export default (state: StateType = initialState, action: Object): StateType => {
  switch (action.type) {
    case SET_MESSAGE:
      return { ...state, messageKey: action.payload.messageKey };
    default:
      return state;
  }
};
