// @flow

import type { ConceptListResolutionResultType } from "../common/types/backend";
import { stripFilename } from "../common/helpers/fileHelper";

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE,
  RESOLVE_FILTER_VALUES_START,
  RESOLVE_FILTER_VALUES_SUCCESS,
  RESOLVE_FILTER_VALUES_ERROR
} from "./actionTypes";

export type StateType = {
  isModalOpen: boolean,
  label: string,
  conceptCodesFromFile: string[],
  selectedConceptRootNode: string,
  loading: boolean,
  resolved: ConceptListResolutionResultType,
  error: ?Error
};

const initialState: StateType = {
  isModalOpen: false,
  label: "",
  conceptCodesFromFile: [],
  selectedConceptRootNode: "",
  loading: false,
  resolved: {},
  error: null
};

const resolveFilterValuesSuccess = (state: StateType, action: Object) => {
  const { data, filename } = action.payload;

  const hasUnresolvedCodes = data.unknownCodes && data.unknownCodes.length > 0;

  return {
    ...state,
    isModalOpen: hasUnresolvedCodes,
    showDetails: false,
    loading: false,
    label: stripFilename(filename),
    resolved: data
  };
};

const uploadConcepts = (state: StateType = initialState, action: Object) => {
  switch (action.type) {
    case UPLOAD_CONCEPT_LIST_MODAL_OPEN:
      const { parameters } = action;

      return {
        ...state,
        isModalOpen: true,
        label: stripFilename(parameters.fileName),
        conceptCodesFromFile: parameters.values,
        selectedConceptRootNode: null,
        resolved: null,
        parameters
      };
    case UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL:
      return {
        ...state,
        label: action.label
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
    case RESOLVE_FILTER_VALUES_START:
      return {
        ...state,
        loading: true,
        error: null
      };
    case RESOLVE_FILTER_VALUES_SUCCESS:
      return resolveFilterValuesSuccess(state, action);
    case RESOLVE_FILTER_VALUES_ERROR:
      return {
        ...state,
        loading: false,
        resolved: null,
        error: action.payload
      };
    case UPLOAD_CONCEPT_LIST_MODAL_CLOSE:
      return initialState;
    default:
      return state;
  }
};

export default uploadConcepts;
