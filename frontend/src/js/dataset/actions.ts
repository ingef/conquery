import { StateT } from "app-types";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { reset } from "redux-form";

import { useGetDatasets } from "../api/api";
import type { DatasetIdT } from "../api/types";
import { defaultError, defaultSuccess } from "../common/actions";
import { exists } from "../common/helpers/exists";
import { useLoadTrees } from "../concept-trees/actions";
import { useLoadQueries } from "../previous-queries/list/actions";
import { setMessage } from "../snack-message/actions";
import { clearQuery, loadSavedQuery } from "../standard-query-editor/actions";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";

import {
  LOAD_DATASETS_START,
  LOAD_DATASETS_SUCCESS,
  LOAD_DATASETS_ERROR,
  SELECT_DATASET,
  SAVE_QUERY,
} from "./actionTypes";
import { setDatasetId } from "./globalDatasetHelper";
import type { DatasetT } from "./reducer";

export const loadDatasetsStart = () => ({ type: LOAD_DATASETS_START });
export const loadDatasetsError = (err: any) =>
  defaultError(LOAD_DATASETS_ERROR, err);
export const loadDatasetsSuccess = (res: any) =>
  defaultSuccess(LOAD_DATASETS_SUCCESS, res);

// Done at the very beginning on loading the site
export const useLoadDatasets = () => {
  const { t } = useTranslation();
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
      dispatch(setMessage({ message: t("datasetSelector.error") }));
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
  query: StandardQueryStateT,
  previouslySelectedDatasetId: DatasetIdT,
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
  const loadQueries = useLoadQueries();

  return (
    datasets: DatasetT[],
    datasetId: DatasetIdT | null,
    previouslySelectedDatasetId: DatasetIdT | null,
    query: StandardQueryStateT,
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
        dispatch(loadSavedQuery({ query: nextDataset.query }));
      }

      loadTrees(datasetId);

      // CLEAR Redux Form
      if (activeForm) {
        dispatch(reset(activeForm));
      }

      return loadQueries(datasetId);
    }
  };
};
