import { SET_MESSAGE, RESET_MESSAGE } from "./actionTypes";

export const setMessage = (message: string | null) => ({
  type: SET_MESSAGE,
  payload: { message },
});

export const resetMessage = () => ({
  type: RESET_MESSAGE,
});
