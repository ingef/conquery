import { getType } from "typesafe-actions";

import type { PostConceptResolveResponseT } from "../api/types";
import { Action } from "../app/actions";
import { stripFilename } from "../common/helpers/fileHelper";

import {
  initUploadConceptListModal,
  resetUploadConceptListModal,
  resolveConcepts,
  selectConceptRootNode,
} from "./actions";

export type UploadConceptListModalStateT = {
  filename: string | null;
  conceptCodesFromFile: string[];
  selectedConceptRootNode: string;
  loading: boolean;
  resolved: PostConceptResolveResponseT;
  error: Error | null;
};

const initialState: UploadConceptListModalStateT = {
  filename: null,
  conceptCodesFromFile: [],
  selectedConceptRootNode: "",
  loading: false,
  resolved: {},
  error: null,
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
        conceptCodesFromFile: rows,
        selectedConceptRootNode: null,
        resolved: null,
      };
    case getType(selectConceptRootNode):
      return {
        ...state,
        selectedConceptRootNode: action.payload.conceptId,
        resolved: null,
      };
    case getType(resolveConcepts.request):
      return {
        ...state,
        loading: true,
        error: null,
      };
    case getType(resolveConcepts.failure):
      return {
        ...state,
        loading: false,
        resolved: null,
        error: action.payload,
      };
    case getType(resolveConcepts.success):
      return {
        ...state,
        loading: false,
        error: null,
        resolved: action.payload.data,
      };
    case getType(resetUploadConceptListModal):
      return initialState;
    default:
      return state;
  }
};

export default uploadConcepts;
