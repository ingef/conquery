import { getType } from "typesafe-actions";

import type { DatasetIdT } from "../api/types";
import type { Action } from "../app/actions";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";

import { loadDatasets, selectDatasetInput, saveQuery } from "./actions";

export type DatasetT = {
  id: DatasetIdT;
  label: string;
  query?: StandardQueryStateT;
};

export type DatasetStateT = {
  pristine: boolean;
  loading: boolean;
  error: string | null;
  data: DatasetT[];
  selectedDatasetId: DatasetIdT | null;
};

const initialState: DatasetStateT = {
  pristine: true,
  loading: false,
  error: null,
  data: [],
  selectedDatasetId: null,
};

const saveQueryInDataset = (
  state: DatasetStateT,
  action: {
    payload: {
      query: StandardQueryStateT;
      previouslySelectedDatasetId: DatasetIdT;
    };
  },
): DatasetStateT => {
  const { query, previouslySelectedDatasetId } = action.payload;

  if (!query || query.length === 0) return state;

  const selectedDataset = state.data.find(
    (db) => db.id === previouslySelectedDatasetId,
  );

  if (!selectedDataset) return state;

  const selectedDatasetIdx = state.data.indexOf(selectedDataset);

  // Save query next to the dataset - so it can be reloaded again
  return {
    ...state,
    data: [
      ...state.data.slice(0, selectedDatasetIdx),
      {
        ...state.data[selectedDatasetIdx],
        query,
      },
      ...state.data.slice(selectedDatasetIdx + 1),
    ],
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
