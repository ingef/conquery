// @flow

import {
  type ConceptListResolutionResultType
} from '../common/types/backend';

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
  label: string,
  conceptCodesFromFile: string[],
  selectedConceptRootNode: string,
  loading: boolean,
  resolved: ConceptListResolutionResultType,
  error: Error
};

const initialState: StateType = {
  isModalOpen: false,
  queryContext: {},
  label: '',
  conceptCodesFromFile: [],
  selectedConceptRootNode: '',
  loading: false,
  resolved: {},
  error: null,
}

const resolveConceptsSuccess = (state: StateType, action: Object) => {
  const { data, treeId, parameters } = action.payload;
  const hasUnresolvedCodes = data.unknownCodes && data.unknownCodes.length > 0;
  const hasResolvedItems = data.conceptCodes && data.conceptCodes.length > 0;

  if (hasUnresolvedCodes)
    return {
      ...state,
      isModalOpen: hasUnresolvedCodes,
      loading: false,
      label: parameters.fileName.replace(/\.[^/.]+$/, ""), // Strip extension from file name
      conceptCodesFromFile: data.conceptCodes,
      selectedConceptRootNode: treeId,
      resolved: data,
      hasUnresolvedCodes: hasUnresolvedCodes,
      hasResolvedItems: hasResolvedItems,
      parameters: parameters
    };

  return {
    ...state,
    loading: false,
    error: null,
    resolved: action.payload.data
  };
}

const uploadConcepts = (state: StateType = initialState, action: Object) => {
  switch (action.type) {
    case UPLOAD_CONCEPT_LIST_MODAL_OPEN:
      const { parameters } = action.data;
      return {
        ...state,
        isModalOpen: true,
        context,
        label: parameters.fileName.replace(/\.[^/.]+$/, ""), // Strip extension from file name
        conceptCodesFromFile: parameters.conceptCodes,
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
      return resolveConceptsSuccess(state, action);
    case UPLOAD_CONCEPT_LIST_MODAL_CLOSE:
      return initialState;
    default:
      return state;
  }
}

export default uploadConcepts;
