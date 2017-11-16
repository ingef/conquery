// @flow

import api                     from '../api';
import { expandPreviousQuery } from '../standard-query-editor/actions';

import {
  loadAllPreviousQueriesInGroups,
} from '../previous-queries/list/actions';

import {
  loadDatasets,
  selectDatasetInput
} from '../dataset/actions';


export const startupOnDataset = (datasetId) =>
  loadDatasets(datasetId);

export const startupOnQuery = (datasetId, queryId) => {
  return (dispatch) => dispatch(loadDatasets(datasetId)).then(
    concepts => {
      if (!concepts)
        return;

      return api.getStoredQuery(datasetId, queryId)
        .then(
          storedQuery => {
            dispatch(expandPreviousQuery(concepts, storedQuery.query.groups));
            dispatch(loadAllPreviousQueriesInGroups(storedQuery.query.groups, datasetId));
          },
          e => dispatch(selectDatasetInput(datasetId)));
    }
  );
}
