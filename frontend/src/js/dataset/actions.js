// @flow

import type { Dispatch } from 'redux-thunk';
import { replace }       from 'react-router-redux';

import { toDataset }     from '../routes';

import api from '../api';

import {
  defaultError,
  defaultSuccess
} from '../common/actions';

import {
  isEmpty,
} from '../common/helpers';

import {
  loadTrees
} from '../category-trees/actions';

import {
  loadPreviousQueries
} from '../previous-queries/list/actions';

import {
  loadQuery,
  clearQuery,
} from '../standard-query-editor/actions';

import {
  type StandardQueryType
} from '../standard-query-editor/types';


import {
  LOAD_DATASETS_START,
  LOAD_DATASETS_SUCCESS,
  LOAD_DATASETS_ERROR,
  SAVE_QUERY,
} from './actionTypes';

import {
  type DatasetType,
  type DatasetIdType,
} from './reducer';


export const loadDatasetsStart = () => ({ type: LOAD_DATASETS_START });
export const loadDatasetsError = (err: any) => defaultError(LOAD_DATASETS_ERROR, err);
export const loadDatasetsSuccess = (res: any) => defaultSuccess(LOAD_DATASETS_SUCCESS, res);

// Done at the very beginning on loading the site
export const loadDatasets = (selectedDatasetId: ?DatasetIdType) => {
  return (dispatch: Dispatch) => {
    dispatch(loadDatasetsStart());

    return api.getDatasets()
      .then(
        r => {
          dispatch(loadDatasetsSuccess(r));

          let firstDatasetId = selectedDatasetId;

          if (selectedDatasetId !== null)
            // Check if the user-provided id is valid
            if (r.find(dataset => dataset.id === selectedDatasetId) === undefined)
              firstDatasetId = null;

          if (firstDatasetId === null && !!r[0])
            // Choose the first dataset from the list
            firstDatasetId = r[0].id;

          if (selectedDatasetId !== firstDatasetId)
            dispatch(selectDatasetInput(firstDatasetId));

          if (firstDatasetId)
            return dispatch(loadTrees(firstDatasetId));
        },
        e => dispatch(loadDatasetsError(e))
      );
  };
};

export const selectDatasetInput = (datasetId: ?DatasetIdType) => {
  if (datasetId && datasetId.length)
    return replace(toDataset(datasetId));
  return replace(("/"));
};

export const saveQuery = (query: StandardQueryType, previouslySelectedDatasetId: DatasetIdType) => {
  return { type: SAVE_QUERY, payload: { query, previouslySelectedDatasetId } };
};

export const selectDataset = (
  datasets: DatasetType[],
  datasetId: DatasetIdType,
  previouslySelectedDatasetId: DatasetIdType,
  query: StandardQueryType
) => {
  return (dispatch: Dispatch) => {
    dispatch(saveQuery(query, previouslySelectedDatasetId));
    dispatch(selectDatasetInput(datasetId));

    // Load query if available, else clear
    if (isEmpty(datasetId)) {
      return dispatch(clearQuery());
    } else {
      const nextDataset = datasets.find(db => db.id === datasetId);

      if (!nextDataset || !nextDataset.query)
        dispatch(clearQuery());
      else
        dispatch(loadQuery(nextDataset.query));

      dispatch(loadTrees(datasetId));

      return dispatch(loadPreviousQueries(datasetId));
    }
  };
};
