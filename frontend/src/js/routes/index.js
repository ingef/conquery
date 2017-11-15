// @flow

import type { DatasetIdType }    from '../dataset/reducer';

export const toDataset = (datasetId: ?DatasetIdType) => {
  if (datasetId === null || datasetId === undefined)
    return "/dataset/:datasetId";

  return `/dataset/${datasetId}`;
}
