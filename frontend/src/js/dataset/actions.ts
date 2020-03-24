import type { Dispatch, getState } from "redux-thunk";
import { reset } from "redux-form";

import api from "../api";
import type { DatasetIdT } from "../api/types";

import { isEmpty } from "../common/helpers";

import { defaultError, defaultSuccess } from "../common/actions";
import { loadTrees } from "../concept-trees/actions";
import { loadPreviousQueries } from "../previous-queries/list/actions";
import { loadQuery, clearQuery } from "../standard-query-editor/actions";
import { setMessage } from "../snack-message/actions";

import type { StandardQueryType } from "../standard-query-editor/types";

import { setDatasetId } from "./globalDatasetHelper";

import {
  LOAD_DATASETS_START,
  LOAD_DATASETS_SUCCESS,
  LOAD_DATASETS_ERROR,
  SELECT_DATASET,
  SAVE_QUERY
} from "./actionTypes";

import type { DatasetT } from "./reducer";

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

      const defaultId = datasets[0].id;

      setDatasetId(defaultId);

      return dispatch(loadTrees(defaultId));
    } catch (e) {
      dispatch(setMessage("datasetSelector.error"));
      dispatch(loadDatasetsError(e));
    }
  };
};

export const selectDatasetInput = (id: DatasetIdT | null) => {
  return {
    type: SELECT_DATASET,
    payload: { id }
  };
};

export const saveQuery = (
  query: StandardQueryType,
  previouslySelectedDatasetId: DatasetIdT
) => {
  return { type: SAVE_QUERY, payload: { query, previouslySelectedDatasetId } };
};

export const selectDataset = (
  datasets: DatasetT[],
  datasetId: DatasetIdT,
  previouslySelectedDatasetId: DatasetIdT,
  query: StandardQueryType
) => {
  return (dispatch: Dispatch, state: getState) => {
    dispatch(saveQuery(query, previouslySelectedDatasetId));

    dispatch(selectDatasetInput(datasetId));

    // To allow loading trees to check whether they should abort or not
    setDatasetId(datasetId);

    // Load query if available, else clear
    if (isEmpty(datasetId)) {
      return dispatch(clearQuery());
    } else {
      const nextDataset = datasets.find(db => db.id === datasetId);

      if (!nextDataset || !nextDataset.query) dispatch(clearQuery());
      else dispatch(loadQuery(nextDataset.query));

      dispatch(loadTrees(datasetId));

      // CLEAR Redux Form
      dispatch(reset(selectActiveForm(state)));

      return dispatch(loadPreviousQueries(datasetId));
    }
  };
};

const selectActiveForm = getStateFn => {
  const state = getStateFn();

  return state.externalForms && state.externalForms.activeForm;
};
