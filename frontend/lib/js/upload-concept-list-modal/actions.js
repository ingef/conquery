// @flow

import api                             from '../api';

import {
  defaultSuccess,
  defaultError
} from '../common/actions';

import { isEmpty }                     from '../common/helpers';

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE,
  UPLOAD_CONCEPT_LIST_MODAL_ACCEPT
} from './actionTypes';

export const resolveConceptsStart = () =>
  ({ type: RESOLVE_CONCEPTS_START });
export const resolveConceptsSuccess = (res) =>
  defaultSuccess(RESOLVE_CONCEPTS_SUCCESS, res);
export const resolveConceptsError = (err) =>
  defaultError(RESOLVE_CONCEPTS_ERROR, err);

export const selectConceptRootNode = (conceptId) =>
  ({ type: SELECT_CONCEPT_ROOT_NODE, conceptId });

export const selectConceptRootNodeAndResolveCodes = (
  datasetId,
  conceptId,
  conceptCodesFromFile
) => {
  return (dispatch) => {
    if (isEmpty(conceptId))
      return dispatch(selectConceptRootNode(null));
    else
      dispatch(selectConceptRootNode(conceptId));

    dispatch(resolveConceptsStart());

    return api.postConceptsListToResolve(datasetId, conceptId, conceptCodesFromFile)
      .then(
        r => dispatch(resolveConceptsSuccess(r)),
        e => dispatch(resolveConceptsError(e))
    );
  }
};

export const uploadConceptListModalUpdateLabel = (label) =>
  ({ type: UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL, label });

export const uploadConceptListModalOpen = (data) => {
  if (data.queryContext.treeId) {
    const datasetId = data.queryContext.datasetId;
    const conceptId = data.queryContext.treeId;
    const conceptCodes = data.conceptCodes;
    return selectConceptRootNodeAndResolveCodes(datasetId, conceptId, conceptCodes);
  }
  return ({ type: UPLOAD_CONCEPT_LIST_MODAL_OPEN, data });
}

export const uploadConceptListModalClose = () =>
  ({ type: UPLOAD_CONCEPT_LIST_MODAL_CLOSE });

export const uploadConceptListModalAccept = (label, rootConcepts, resolutionResult, queryContext) =>
  ({
    type: UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
    data: { label, rootConcepts, resolutionResult, queryContext }
  });

export const acceptAndCloseUploadConceptListModal = (
  label,
  rootConcepts,
  resolutionResult,
  queryContext
) => {
  return (dispatch) => {
    dispatch([
      uploadConceptListModalAccept(label, rootConcepts, resolutionResult, queryContext),
      uploadConceptListModalClose()
    ]);
  }
};

