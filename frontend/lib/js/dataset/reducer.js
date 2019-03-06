// @flow

import {
  LOAD_DATASETS_START,
  LOAD_DATASETS_SUCCESS,
  LOAD_DATASETS_ERROR,
  SAVE_QUERY
} from "./actionTypes";

export type DatasetIdType = string;

export type DatasetType = {
  id: DatasetIdType,
  label: string
};

export type StateType = {
  loading: boolean,
  error: ?string,
  data: DatasetType[]
};

const initialState: StateType = {
  loading: false,
  error: null,
  data: []
};

const saveQuery = (state: StateType, action: Object): StateType => {
  const { query, previouslySelectedDatasetId } = action.payload;

  if (!query || query.length === 0) return state;

  const selectedDataset = state.data.find(
    db => db.id === previouslySelectedDatasetId
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
        query
      },
      ...state.data.slice(selectedDatasetIdx + 1)
    ]
  };
};

const datasets = (
  state: StateType = initialState,
  action: Object
): StateType => {
  switch (action.type) {
    // To start a query
    case LOAD_DATASETS_START:
      return { ...state, loading: true };
    case LOAD_DATASETS_SUCCESS:
      return { ...state, loading: false, data: action.payload.data };
    case LOAD_DATASETS_ERROR:
      return { ...state, loading: false, error: action.payload.message };
    case SAVE_QUERY:
      return saveQuery(state, action);
    default:
      return state;
  }
};

export default datasets;
