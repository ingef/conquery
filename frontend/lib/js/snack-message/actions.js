// @flow

import { SET_MESSAGE, RESET_MESSAGE } from "./actionTypes";

export const setMessage = (messageKey: ?string) => ({
  type: SET_MESSAGE,
  payload: { messageKey }
});

export const resetMessage = () => ({
  type: RESET_MESSAGE
});
