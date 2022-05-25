import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import { useGetDatasets } from "../api/api";
import type { DatasetIdT, GetDatasetsResponseT } from "../api/types";
import { ErrorObject } from "../common/actions";
import { exists } from "../common/helpers/exists";
import { useLoadTrees } from "../concept-trees/actions";
import { useLoadDefaultHistoryParams } from "../entity-history/actions";
import { useLoadQueries } from "../previous-queries/list/actions";
import { setMessage } from "../snack-message/actions";
import { clearQuery, loadSavedQuery } from "../standard-query-editor/actions";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";

import { setDatasetId } from "./globalDatasetHelper";
import type { DatasetT } from "./reducer";

export type DatasetActions = ActionType<
  typeof loadDatasets | typeof selectDatasetInput | typeof saveQuery
>;

export const loadDatasets = createAsyncAction(
  "dataset/LOAD_DATASETS_START",
  "dataset/LOAD_DATASETS_SUCCESS",
  "dataset/LOAD_DATASETS_ERROR",
)<void, { datasets: GetDatasetsResponseT }, ErrorObject>();

// Done at the very beginning on loading the site
export const useLoadDatasets = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const getDatasets = useGetDatasets();
  const loadTrees = useLoadTrees();
  const loadDefaultHistoryParams = useLoadDefaultHistoryParams();

  return async () => {
    dispatch(loadDatasets.request());

    try {
      const datasets = await getDatasets();

      if (!datasets || datasets.length === 0 || !datasets[0].id) {
        throw new Error("No valid dataset found");
      }

      dispatch(loadDatasets.success({ datasets }));

      const defaultId = datasets[0].id;

      setDatasetId(defaultId);

      loadDefaultHistoryParams(defaultId);

      return loadTrees(defaultId);
    } catch (e) {
      dispatch(setMessage({ message: t("datasetSelector.error") }));
      dispatch(loadDatasets.failure(e as Error));
    }
  };
};

export const selectDatasetInput =
  createAction("dataset/SELECT")<{ id: DatasetIdT | null }>();

export const saveQuery = createAction("dataset/SAVE_QUERY")<{
  query: StandardQueryStateT;
  previouslySelectedDatasetId: DatasetIdT;
}>();

export const useSelectDataset = () => {
  const dispatch = useDispatch();
  const loadTrees = useLoadTrees();
  const { loadQueries } = useLoadQueries();

  return (
    datasets: DatasetT[],
    datasetId: DatasetIdT | null,
    previouslySelectedDatasetId: DatasetIdT | null,
    query: StandardQueryStateT,
  ) => {
    if (previouslySelectedDatasetId) {
      dispatch(saveQuery({ query, previouslySelectedDatasetId }));
    }

    dispatch(selectDatasetInput({ id: datasetId }));

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

      return loadQueries(datasetId);
    }
  };
};
