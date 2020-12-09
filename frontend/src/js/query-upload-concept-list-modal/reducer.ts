import { ActionT } from "../common/actions";
import { MODAL_OPEN, MODAL_CLOSE } from "./actionTypes";

interface QueryUploadConceptListModalStateT {
  isOpen: boolean;
  andIdx: number | null;
}

const initialState: QueryUploadConceptListModalStateT = {
  isOpen: false,
  andIdx: null,
};

export default (
  state = initialState,
  action: ActionT
): QueryUploadConceptListModalStateT => {
  switch (action.type) {
    case MODAL_OPEN:
      const { andIdx } = action.payload;

      return { andIdx, isOpen: true };
    case MODAL_CLOSE:
      return initialState;
    default:
      return state;
  }
};
