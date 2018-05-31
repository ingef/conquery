// @flow

import type { Dispatch }               from 'redux';

import api                             from '../api';
import {
  defaultSuccess,
  defaultError
}                                      from '../common/actions';
import { isEmpty }                     from '../common/helpers';
import { type TreeNodeIdType }         from '../common/types/backend';
import { type GenericFileType }        from '../file-upload/types';

import {
  UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL,
  SELECT_CONCEPT_ROOT_NODE,
  RESOLVE_CONCEPTS_START,
  RESOLVE_CONCEPTS_SUCCESS,
  RESOLVE_CONCEPTS_ERROR,
  UPLOAD_CONCEPT_LIST_MODAL_OPEN,
  UPLOAD_CONCEPT_LIST_MODAL_CLOSE,
  UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
  RESOLVE_FILTER_VALUES_SUCCESS,
  RESOLVE_FILTER_VALUES_ERROR
}                                      from './actionTypes';

export const resolveConceptsStart = () =>
  ({ type: RESOLVE_CONCEPTS_START });
export const resolveConceptsSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_CONCEPTS_SUCCESS, res, payload);
export const resolveConceptsError = (err: any) =>
  defaultError(RESOLVE_CONCEPTS_ERROR, err);

  export const resolveFilterValuesSuccess = (res: any, payload?: Object) =>
  defaultSuccess(RESOLVE_FILTER_VALUES_SUCCESS, res, payload);
  export const resolveFilterValuesError = (err: any) =>
  defaultError(RESOLVE_FILTER_VALUES_ERROR, err);

export const selectConceptRootNode = (conceptId: TreeNodeIdType) =>
  ({ type: SELECT_CONCEPT_ROOT_NODE, conceptId });

export const selectConceptRootNodeAndResolveCodes = (parameters: Object) => {
  const { treeId, datasetId, conceptCodes } = parameters;

  return (dispatch: Dispatch<*>) => {
    if (isEmpty(treeId))
      return dispatch(selectConceptRootNode(''));
    else
      dispatch(selectConceptRootNode(treeId));

    dispatch(resolveConceptsStart());

    return api.postConceptsListToResolve(datasetId, treeId, conceptCodes)
      .then(
        r => dispatch(resolveConceptsSuccess(r)),
        e => dispatch(resolveConceptsError(e))
    );
  }
};

export const conceptFilterValuesResolve = (fileType: GenericFileType) => {
  const datasetId = fileType.parameters.datasetId;
  const treeId = fileType.parameters.treeId;
  const tableId = fileType.parameters.tableId;
  const filterId = fileType.parameters.filterId;
  const values = fileType.parameters.values;

  return (dispatch: Dispatch<*>) => {
    dispatch(resolveConceptsStart());

    return api.postConceptFilterValuesResolve(datasetId, treeId, tableId, filterId, values)
      .then(
        r => dispatch(resolveFilterValuesSuccess(r, {...fileType})),
        e => dispatch(resolveFilterValuesError(e))
    );
  }
}

export const uploadConceptListModalUpdateLabel = (label: string) =>
  ({ type: UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL, label });

export const uploadConceptListModalOpen = (fileType: GenericFileType) => {
  const parameters = fileType.parameters;
  if (parameters.treeId)
    return fileType.callback && fileType.callback(parameters);

  return ({ type: UPLOAD_CONCEPT_LIST_MODAL_OPEN, parameters });
}

export const uploadConceptListModalClose = () =>
  ({ type: UPLOAD_CONCEPT_LIST_MODAL_CLOSE });

export const uploadConceptListModalAccept = (
  label,
  rootConcepts,
  resolutionResult,
  parameters
) => ({
    type: parameters.actionType || UPLOAD_CONCEPT_LIST_MODAL_ACCEPT,
    data: { label, rootConcepts, resolutionResult, parameters }
  });

export const acceptAndCloseUploadConceptListModal = (
  label,
  rootConcepts,
  resolutionResult,
  parameters
) => {
  return (dispatch) => {
    dispatch([
      uploadConceptListModalAccept(label, rootConcepts, resolutionResult, parameters),
      uploadConceptListModalClose()
    ]);
  }
};
