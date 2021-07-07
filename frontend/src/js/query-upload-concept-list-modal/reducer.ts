import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import {
  closeQueryUploadConceptListModal,
  openQueryUploadConceptListModal,
} from "./actions";

export interface QueryUploadConceptListModalStateT {
  isOpen: boolean;
  andIdx: number | null;
}

const initialState: QueryUploadConceptListModalStateT = {
  isOpen: false,
  andIdx: null,
};

const reducer = (
  state = initialState,
  action: Action,
): QueryUploadConceptListModalStateT => {
  switch (action.type) {
    case getType(openQueryUploadConceptListModal):
      const { andIdx } = action.payload;

      return { andIdx, isOpen: true };
    case getType(closeQueryUploadConceptListModal):
      return initialState;
    default:
      return state;
  }
};

export default reducer;
