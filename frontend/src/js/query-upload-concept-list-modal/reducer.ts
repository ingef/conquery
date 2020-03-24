import { MODAL_OPEN, MODAL_CLOSE } from "./actionTypes";

type StateT = {
  isOpen: boolean;
  andIdx: number;
};

const initialState: StateT = {
  isOpen: false,
  andIdx: null
};

export default (state = initialState, action) => {
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
