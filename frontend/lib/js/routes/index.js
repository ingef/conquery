// @flow

import type { DatasetIdType } from "../dataset/reducer";

export const templates = {
  toDataset: "/dataset/:datasetId",
  toQuery: "/dataset/:datasetId/query/:queryId"
};

export const toDataset = (datasetId: DatasetIdType) => {
  return `/dataset/${datasetId}`;
};

export const toQuery = (datasetId: DatasetIdType, queryId: string) => {
  return `/dataset/${datasetId}/query/${queryId}`;
};
