// @flow

import type { PostConceptResolveResponseT } from "../api/types";
import { stripFilename } from "../common/helpers/fileHelper";

import {
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE
} from "./actionTypes";

export type StateType = {
  andIdx: ?number,
  isModalOpen: boolean,
  filename: ?string,
  conceptCodesFromFile: string[],
  selectedConceptRootNode: string,
  loading: boolean,
  resolved: PostConceptResolveResponseT,
  error: ?Error
};

const initialState: StateType = {
  andIdx: null,
  isModalOpen: false,
  filename: null,
  conceptCodesFromFile: [],
  selectedConceptRootNode: "",
  loading: false,
  resolved: {},
  error: null
};

const uploadConcepts = (state: StateType = initialState, action: Object) => {
  switch (action.type) {
    case UPLOAD_CONCEPT_LIST_MODAL_OPEN:
      const { andIdx, filename, rows } = action.payload;

      return {
        ...state,
        andIdx,
        isModalOpen: true,
        filename: stripFilename(filename),
        conceptCodesFromFile: rows,
        selectedConceptRootNode: null,
        resolved: null
      };
    case SELECT_CONCEPT_ROOT_NODE:
      return {
        ...state,
        selectedConceptRootNode: action.conceptId,
        resolved: null
      };
    case RESOLVE_CONCEPTS_START:
      return {
        ...state,
        loading: true,
        error: null
      };
    case RESOLVE_CONCEPTS_ERROR:
      return {
        ...state,
        loading: false,
        resolved: null,
        error: action.payload
      };
    case RESOLVE_CONCEPTS_SUCCESS:
      return {
        ...state,
        loading: false,
        error: null,
        resolved: action.payload.data
      };
    case UPLOAD_CONCEPT_LIST_MODAL_CLOSE:
      return initialState;
    default:
      return state;
  }
};

export default uploadConcepts;
