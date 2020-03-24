import type { PostConceptResolveResponseT } from "../api/types";
import { stripFilename } from "../common/helpers/fileHelper";

import {
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  INIT,
  RESET
} from "./actionTypes";

export type StateType = {
  filename: string | null,
  conceptCodesFromFile: string[],
  selectedConceptRootNode: string,
  loading: boolean,
  resolved: PostConceptResolveResponseT,
  error: Error | null
};

const initialState: StateType = {
  filename: null,
  conceptCodesFromFile: [],
  selectedConceptRootNode: "",
  loading: false,
  resolved: {},
  error: null
};

const uploadConcepts = (state: StateType = initialState, action: Object) => {
  switch (action.type) {
    case INIT:
      const { filename, rows } = action.payload;

      return {
        ...state,
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
    case RESET:
      return initialState;
    default:
      return state;
  }
};

export default uploadConcepts;
