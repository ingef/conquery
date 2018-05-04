// @flow

import type { Dispatch }               from 'redux';

import api                             from '../api';
import {
  defaultSuccess,
  defaultError
}                                      from '../common/actions';
import { isEmpty }                     from '../common/helpers';
import { type TreeNodeIdType }         from '../common/types/backend';
import { type ConceptFileType }        from '../file-dnd/types';

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE,
  UPLOAD_CONCEPT_LIST_MODAL_ACCEPT
}                                      from './actionTypes';

export const resolveConceptsStart = () =>
  ({ type: RESOLVE_CONCEPTS_START });
export const resolveConceptsSuccess = (res: any) =>
  defaultSuccess(RESOLVE_CONCEPTS_SUCCESS, res);
export const resolveConceptsError = (err: any) =>
  defaultError(RESOLVE_CONCEPTS_ERROR, err);

export const selectConceptRootNode = (conceptId: TreeNodeIdType) =>
  ({ type: SELECT_CONCEPT_ROOT_NODE, conceptId });

export const selectConceptRootNodeAndResolveCodes = (parameters: Object) => {
  const conceptId = parameters['treeId'];
  const datasetId = parameters['datasetId'];
  const conceptCodes = parameters['conceptCodes'];

  return (dispatch: Dispatch<*>) => {
    if (isEmpty(conceptId))
      return dispatch(selectConceptRootNode(''));
    else
      dispatch(selectConceptRootNode(conceptId));

    dispatch(resolveConceptsStart());

    return api.postConceptsListToResolve(datasetId, conceptId, conceptCodes)
      .then(
        r => dispatch(resolveConceptsSuccess(r)),
        e => dispatch(resolveConceptsError(e))
    );
  }
};

export const conceptFilterValuesResolve = (parameters: Object) => {
  const datasetId = parameters['datasetId'];
  const treeId = parameters['treeId'];
  const tableId = parameters['tableId'];
  const filterId = parameters['filterId'];
  const values = parameters['values'];

  return (dispatch: Dispatch<*>) => {
    return api.postConceptFilterValuesResolve(datasetId, treeId, tableId, filterId, values)
      .then(
        r => dispatch(resolveConceptsSuccess(r)),
        e => dispatch(resolveConceptsError(e))
    );
  }
}

export const uploadConceptListModalUpdateLabel = (label: string) =>
  ({ type: UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL, label });

export const uploadConceptListModalOpen = (data: ConceptFileType) => {
  if (data.parameters['treeId'])
    return data.callback && data.callback(data.parameters);

  return ({ type: UPLOAD_CONCEPT_LIST_MODAL_OPEN, data });
}

export const uploadConceptListModalClose = () =>
  ({ type: UPLOAD_CONCEPT_LIST_MODAL_CLOSE });

export const uploadConceptListModalAccept = (
  label,
  rootConcepts,
  resolutionResult,
  queryContext
) => ({
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

