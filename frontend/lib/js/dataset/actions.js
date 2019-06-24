// @flow

import type { Dispatch, getState } from "redux-thunk";
import { reset } from "redux-form";

import api from "../api";

import { isEmpty } from "../common/helpers";

import { defaultError, defaultSuccess } from "../common/actions";
import { loadTrees } from "../concept-trees/actions";
import { loadPreviousQueries } from "../previous-queries/list/actions";
import { loadQuery, clearQuery } from "../standard-query-editor/actions";
import { setMessage } from "../snack-message/actions";

import { type StandardQueryType } from "../standard-query-editor/types";

import {
  LOAD_DATASETS_START,
  LOAD_DATASETS_SUCCESS,
  LOAD_DATASETS_ERROR,
  SELECT_DATASET,
  SAVE_QUERY
} from "./actionTypes";

import { type DatasetType, type DatasetIdType } from "./reducer";

export const loadDatasetsStart = () => ({ type: LOAD_DATASETS_START });
export const loadDatasetsError = (err: any) =>
  defaultError(LOAD_DATASETS_ERROR, err);
export const loadDatasetsSuccess = (res: any) =>
  defaultSuccess(LOAD_DATASETS_SUCCESS, res);

// Done at the very beginning on loading the site
export const loadDatasets = () => {
  return async (dispatch: Dispatch) => {
    dispatch(loadDatasetsStart());

    try {
      const datasets = await api.getDatasets();

      if (!datasets || datasets.length === 0 || !datasets[0].id) {
        throw new Error("No valid dataset found");
      }

      dispatch(loadDatasetsSuccess(datasets));

      return dispatch(loadTrees(datasets[0].id));
    } catch (e) {
      dispatch(setMessage("datasetSelector.error"));
      dispatch(loadDatasetsError(e));
    }
  };
};

export const selectDatasetInput = (id: ?DatasetIdType) => {
  return {
    type: SELECT_DATASET,
    payload: { id }
  };
};

export const saveQuery = (
  query: StandardQueryType,
  previouslySelectedDatasetId: DatasetIdType
) => {
  return { type: SAVE_QUERY, payload: { query, previouslySelectedDatasetId } };
};

export const selectDataset = (
  datasets: DatasetType[],
  datasetId: DatasetIdType,
  previouslySelectedDatasetId: DatasetIdType,
  query: StandardQueryType
) => {
  return (dispatch: Dispatch, state: getState) => {
    dispatch(saveQuery(query, previouslySelectedDatasetId));
    dispatch(selectDatasetInput(datasetId));

    // Load query if available, else clear
    if (isEmpty(datasetId)) {
      return dispatch(clearQuery());
    } else {
      const nextDataset = datasets.find(db => db.id === datasetId);

      if (!nextDataset || !nextDataset.query) dispatch(clearQuery());
      else dispatch(loadQuery(nextDataset.query));

      dispatch(loadTrees(datasetId));
      // clearing Redux Form
      dispatch(reset(selectActiveForm(state)));

      return dispatch(loadPreviousQueries(datasetId));
    }
  };
};

const selectActiveForm = getState => {
  const state = getState();
  return (
    state.panes &&
    state.panes.right &&
    state.panes.right.tabs &&
    state.panes.right.tabs.externalForms &&
    state.panes.right.tabs.externalForms.externalForms &&
    state.panes.right.tabs.externalForms.externalForms.activeForm
  );
};
