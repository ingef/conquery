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
export const loadDatasets = (datasetIdFromUrl: ?DatasetIdType) => {
  return (dispatch: Dispatch) => {
    dispatch(loadDatasetsStart());

    return api.getDatasets()
      .then(
        datasets => {
          dispatch(loadDatasetsSuccess(datasets));

          let selectedDatasetId = datasetIdFromUrl;

          if (datasetIdFromUrl !== null)
            // Check if the user-provided id is valid
            if (!datasets.find(dataset => dataset.id === datasetIdFromUrl))
              selectedDatasetId = null;

          // Default to the first dataset from the list
          if (selectedDatasetId === null && !!datasets[0])
            selectedDatasetId = datasets[0].id;

          if (datasetIdFromUrl !== selectedDatasetId)
            dispatch(selectDatasetInput(selectedDatasetId));

          if (selectedDatasetId)
            return dispatch(loadTrees(selectedDatasetId));
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
