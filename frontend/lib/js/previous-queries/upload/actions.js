// @flow

import T                        from 'i18n-react';
import { type Dispatch }        from 'redux-thunk';

import { type DatasetIdType }   from '../../dataset/reducer';
import {
  defaultSuccess,
  defaultError
}                               from '../../common/actions';
import api                      from '../../api';
import { QUERY_AGAIN_TIMEOUT }  from '../../query-runner/constants';
import { getStoredQuery }       from '../../api/api';

import { loadPreviousQueries }  from '../list/actions';

import {
  OPEN_UPLOAD_MODAL,
  CLOSE_UPLOAD_MODAL,
  UPLOAD_FILE_START,
  UPLOAD_FILE_SUCCESS,
  UPLOAD_FILE_ERROR,
}                             from './actionTypes';

export const openUploadModal = () => ({
  type: OPEN_UPLOAD_MODAL
});

export const closeUploadModal = () => ({
  type: CLOSE_UPLOAD_MODAL
});

export const uploadFileStart = () =>
  ({ type: UPLOAD_FILE_START });
export const uploadFileSuccess = (success: any) =>
  defaultSuccess(UPLOAD_FILE_SUCCESS, success);
export const uploadFileError = (error: any, payload: Object) =>
  defaultError(UPLOAD_FILE_ERROR, error, {
    successful: payload.successful,
    unsuccessful: payload.unsuccessful,
  });

export const uploadFile = (datasetId: DatasetIdType, file: any) => (dispatch: Dispatch) => {
  dispatch(uploadFileStart());

  return api.postResults(datasetId, file).then(
    r => {
      if (r.status === 'RUNNING' || r.status === 'NEW')
        dispatch(waitForQueryDone(datasetId, r.id));

      if (r.status === 'FAILED' || r.status === 'CANCELED')
        dispatch(uploadFailed(r));

      if (r.status === 'DONE')
        dispatch([uploadFileSuccess(r), loadPreviousQueries(datasetId)]);
    },
    e => dispatch(uploadFailed(e))
  );
}

const uploadFailed = (result: any) => uploadFileError({
    message: T.translate('uploadQueryResultsModal.uploadFailed')
  }, result);

const waitForQueryDone = (datasetId, queryId) => (dispatch: Dispatch) =>
  getStoredQuery(datasetId, queryId).then(
    r => {
      if (r.status === 'DONE' && r.message)
        return dispatch(uploadFailed(r));
      else if (r.status === 'DONE')
        return dispatch([uploadFileSuccess(r), loadPreviousQueries(datasetId)]);
      else
        setTimeout(
          () => waitForQueryDone(datasetId, queryId),
          QUERY_AGAIN_TIMEOUT
        );
    }
  )
