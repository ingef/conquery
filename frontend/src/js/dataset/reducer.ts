import { getType } from "typesafe-actions";

import type { DatasetT } from "../api/types";
import type { Action } from "../app/actions";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";

import { loadDatasets, saveQuery, selectDatasetInput } from "./actions";

export type DatasetStateT = {
  pristine: boolean;
  loading: boolean;
  error: string | null;
  data: DatasetT[];
  selectedDatasetId: DatasetT["id"] | null;
  locallySavedQueries: {
    [datasetId: DatasetT["id"]]: StandardQueryStateT;
  };
};

const initialState: DatasetStateT = {
  pristine: true,
  loading: false,
  error: null,
  data: [],
  selectedDatasetId: null,
  locallySavedQueries: {},
};

const saveQueryInDataset = (
  state: DatasetStateT,
  action: {
    payload: {
      query: StandardQueryStateT;
      previouslySelectedDatasetId: DatasetT["id"];
    };
  },
): DatasetStateT => {
  const { query, previouslySelectedDatasetId } = action.payload;

  if (!query || query.length === 0) return state;

  return {
    ...state,
    locallySavedQueries: {
      ...state.locallySavedQueries,
      [previouslySelectedDatasetId]: query,
    },
  };
};

const datasets = (
  state: DatasetStateT = initialState,
  action: Action,
): DatasetStateT => {
  switch (action.type) {
    case getType(loadDatasets.request):
      return { ...state, loading: true, pristine: false };
    case getType(loadDatasets.success):
      const { datasets } = action.payload;
      const selectedDatasetId =
        datasets && datasets.length > 0 ? datasets[0].id : null;

      return {
        ...state,
        loading: false,
        error: null,
        data: datasets,
        selectedDatasetId,
      };
    case getType(loadDatasets.failure):
      return {
        ...state,
        loading: false,
        error: action.payload.message || null,
      };
    case getType(selectDatasetInput):
      return {
        ...state,
        selectedDatasetId: action.payload.id,
      };
    case getType(saveQuery):
      return saveQueryInDataset(state, action);
    default:
      return state;
  }
};

export default datasets;
