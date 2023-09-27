import { getType } from "typesafe-actions";

import { Action } from "../app/actions";

import { openPreview, closePreview } from "./actions";

export type PreviewStateT = {
  isOpen: boolean;
};

const initialState: PreviewStateT = {
  isOpen: false,
};

export default function reducer(
  state: PreviewStateT = initialState,
  action: Action,
): PreviewStateT {
  switch (action.type) {
    case getType(openPreview):
      return {
        ...state,
        isOpen: true,
      };
    case getType(closePreview):
      return {
        ...state,
        isOpen: false,
      };
    default:
      return state;
  }
}
