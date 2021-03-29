import { SET_SELECTED_SECONDARY_ID } from "./actionTypes";

export type SelectedSecondaryIdStateT = string | null;

const reducer = (state: SelectedSecondaryIdStateT = null, action: any) => {
  switch (action.type) {
    case SET_SELECTED_SECONDARY_ID:
      return action.payload.secondaryId;
    default:
      return state;
  }
};

export default reducer;
