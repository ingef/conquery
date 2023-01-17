import { getType } from "typesafe-actions";

import { Action } from "../app/actions";
import { stripFilename } from "../common/helpers/fileHelper";

import {
  initUploadConceptListModal,
  resetUploadConceptListModal,
} from "./actions";

export type UploadConceptListModalStateT = {
  filename: string | null;
  fileRows: string[];
};

const initialState: UploadConceptListModalStateT = {
  filename: null,
  fileRows: [],
};

const uploadConcepts = (
  state: UploadConceptListModalStateT = initialState,
  action: Action,
) => {
  switch (action.type) {
    case getType(initUploadConceptListModal):
      const { filename, rows } = action.payload;

      return {
        ...state,
        filename: stripFilename(filename),
        fileRows: rows,
        resolved: null,
      };
    case getType(resetUploadConceptListModal):
      return initialState;
    default:
      return state;
  }
};

export default uploadConcepts;
