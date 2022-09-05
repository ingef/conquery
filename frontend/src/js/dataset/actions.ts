import { useCallback, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction, createAsyncAction } from "typesafe-actions";

import { useGetDatasets } from "../api/api";
import type { DatasetT, GetDatasetsResponseT } from "../api/types";
import { StateT } from "../app/reducers";
import { ErrorObject } from "../common/actions";
import { exists } from "../common/helpers/exists";
import { useLoadTrees } from "../concept-trees/actions";
import { useLoadDefaultHistoryParams } from "../entity-history/actions";
import { useLoadQueries } from "../previous-queries/list/actions";
import { setMessage } from "../snack-message/actions";
import { clearQuery, loadSavedQuery } from "../standard-query-editor/actions";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";

import { setDatasetId } from "./globalDatasetHelper";
import type { DatasetStateT } from "./reducer";
import { useDatasetId } from "./selectors";

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

  return useCallback(async () => {
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
  }, [dispatch, getDatasets, loadDefaultHistoryParams, loadTrees, t]);
};

export const selectDatasetInput =
  createAction("dataset/SELECT")<{ id: DatasetT["id"] | null }>();

export const saveQuery = createAction("dataset/SAVE_QUERY")<{
  query: StandardQueryStateT;
  previouslySelectedDatasetId: DatasetT["id"];
}>();

export const useSelectDataset = () => {
  const dispatch = useDispatch();
  const loadTrees = useLoadTrees();
  const { loadQueries } = useLoadQueries();
  const previouslySelectedDatasetId = useDatasetId();
  const locallySavedQueries = useSelector<
    StateT,
    DatasetStateT["locallySavedQueries"]
  >((state) => state.datasets.locallySavedQueries);

  const loadDefaultHistoryParams = useLoadDefaultHistoryParams();

  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );
  // Avoid constant re-renders when updating the query
  const queryRef = useRef(query);
  useEffect(() => {
    queryRef.current = query;
  }, [query]);

  return useCallback(
    (datasetId: DatasetT["id"] | null) => {
      if (previouslySelectedDatasetId) {
        dispatch(
          saveQuery({ query: queryRef.current, previouslySelectedDatasetId }),
        );
      }

      dispatch(selectDatasetInput({ id: datasetId }));

      // To allow loading trees to check whether they should abort or not
      setDatasetId(datasetId);

      // Load query if available, else clear
      if (!exists(datasetId)) {
        return dispatch(clearQuery());
      } else {
        const nextDatasetSavedQuery = locallySavedQueries[datasetId];

        if (!nextDatasetSavedQuery) {
          dispatch(clearQuery());
        } else {
          dispatch(loadSavedQuery({ query: nextDatasetSavedQuery }));
        }

        loadTrees(datasetId);
        loadDefaultHistoryParams(datasetId);

        return loadQueries(datasetId);
      }
    },
    [
      dispatch,
      loadTrees,
      loadQueries,
      locallySavedQueries,
      previouslySelectedDatasetId,
    ],
  );
};
