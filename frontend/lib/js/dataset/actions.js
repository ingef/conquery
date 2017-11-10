// @flow

import type { Dispatch } from 'redux-thunk';

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
  SELECT_DATASET,
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
export const loadDatasets = () => {
  return (dispatch: Dispatch) => {
    dispatch(loadDatasetsStart());

    return api.getDatasets()
      .then(
        r => {
          dispatch(loadDatasetsSuccess(r));

          if (!r[0]) return r;

          const firstDatasetId = r[0].id;

          if (firstDatasetId === undefined) return r;

          dispatch(selectDatasetInput(firstDatasetId));

          return dispatch(loadTrees(firstDatasetId));
        },
        e => dispatch(loadDatasetsError(e))
      );
  };
};

export const selectDatasetInput = (datasetId: DatasetIdType) => {
  return { type: SELECT_DATASET, payload: { datasetId } };
};

export const saveQuery = (query: StandardQueryType) => {
  return { type: SAVE_QUERY, payload: { query } };
};

export const selectDataset = (
  datasets: DatasetType[],
  datasetId: DatasetIdType,
  query: StandardQueryType
) => {
  return (dispatch: Dispatch) => {
    dispatch(saveQuery(query));
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
