// @flow

import { SET_MESSAGE } from "./actionTypes";

export const setMessage = (messageKey: ?string) => ({
  type: SET_MESSAGE,
  payload: { messageKey }
});
