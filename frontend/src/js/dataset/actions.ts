import { reset } from "redux-form";

import type { DatasetIdT } from "../api/types";

import { defaultError, defaultSuccess } from "../common/actions";
import { useLoadTrees } from "../concept-trees/actions";
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
  SAVE_QUERY,
} from "./actionTypes";

import type { DatasetT } from "./reducer";
import { exists } from "../common/helpers/exists";
import { useDispatch, useSelector } from "react-redux";
import { useGetDatasets } from "../api/api";
import { StateT } from "app-types";

export const loadDatasetsStart = () => ({ type: LOAD_DATASETS_START });
export const loadDatasetsError = (err: any) =>
  defaultError(LOAD_DATASETS_ERROR, err);
export const loadDatasetsSuccess = (res: any) =>
  defaultSuccess(LOAD_DATASETS_SUCCESS, res);

// Done at the very beginning on loading the site
export const useLoadDatasets = () => {
  const dispatch = useDispatch();
  const getDatasets = useGetDatasets();
  const loadTrees = useLoadTrees();

  return async () => {
    dispatch(loadDatasetsStart());

    try {
      const datasets = await getDatasets();

      if (!datasets || datasets.length === 0 || !datasets[0].id) {
        throw new Error("No valid dataset found");
      }

      dispatch(loadDatasetsSuccess(datasets));

      const defaultId = datasets[0].id;

      setDatasetId(defaultId);

      return loadTrees(defaultId);
    } catch (e) {
      dispatch(setMessage("datasetSelector.error"));
      dispatch(loadDatasetsError(e));
    }
  };
};

export const selectDatasetInput = (id: DatasetIdT | null) => {
  return {
    type: SELECT_DATASET,
    payload: { id },
  };
};

export const saveQuery = (
  query: StandardQueryType,
  previouslySelectedDatasetId: DatasetIdT
) => {
  return { type: SAVE_QUERY, payload: { query, previouslySelectedDatasetId } };
};

const selectActiveForm = (state: StateT) => {
  return state.externalForms && state.externalForms.activeForm;
};

export const useSelectDataset = () => {
  const dispatch = useDispatch();
  const loadTrees = useLoadTrees();
  const activeForm = useSelector<StateT, string | null>(selectActiveForm);

  return (
    datasets: DatasetT[],
    datasetId: DatasetIdT | null,
    previouslySelectedDatasetId: DatasetIdT | null,
    query: StandardQueryType
  ) => {
    if (previouslySelectedDatasetId) {
      dispatch(saveQuery(query, previouslySelectedDatasetId));
    }

    dispatch(selectDatasetInput(datasetId));

    // To allow loading trees to check whether they should abort or not
    setDatasetId(datasetId);

    // Load query if available, else clear
    if (!exists(datasetId)) {
      return dispatch(clearQuery());
    } else {
      const nextDataset = datasets.find((db) => db.id === datasetId);

      if (!nextDataset || !nextDataset.query) {
        dispatch(clearQuery());
      } else {
        dispatch(loadQuery(nextDataset.query));
      }

      dispatch(loadTrees(datasetId));

      // CLEAR Redux Form
      if (activeForm) {
        dispatch(reset(activeForm));
      }

      return dispatch(loadPreviousQueries(datasetId));
    }
  };
};
