// @flow

import {
  type ConceptListResolutionResultType
} from '../api/api';

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE
} from './actionTypes'

type QueryContextType = {
  andIdx?: Number,
  dateRange?: any
};

export type StateType = {
  isModalOpen: boolean,
  queryContext: QueryContextType,
  label: String,
  conceptCodesFromFile: String[],
  selectedConceptRootNode: String,
  loading: boolean,
  resolved: ConceptListResolutionResultType,
  error: Error
};

const initialState: StateType = {
  isModalOpen: false,
  queryContext: null,
  label: null,
  conceptCodesFromFile: [],
  selectedConceptRootNode: null,
  loading: false,
  resolved: null,
  error: null,
}

const uploadConcepts = (state = initialState, action) => {
  switch (action.type) {
    case UPLOAD_CONCEPT_LIST_MODAL_OPEN:
      const { fileName, conceptCodes, queryContext } = action.data;
      return {
        ...state,
        isModalOpen: true,
        queryContext,
        label: fileName.replace(/\.[^/.]+$/, ""), // Strip extension from file name
        conceptCodesFromFile: conceptCodes,
        selectedConceptRootNode: null,
        resolved: null
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
    case UPLOAD_CONCEPT_LIST_MODAL_CLOSE:
      return initialState;
    default:
      return state;
  }
}

export default uploadConcepts;
