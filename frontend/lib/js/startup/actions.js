// @flow

import type { Dispatch } from "redux-thunk";

import api from "../api";
// import { expandPreviousQuery } from "../standard-query-editor/actions";
// import { loadAllPreviousQueriesInGroups } from "../previous-queries/list/actions";
import { loadDatasets } from "../dataset/actions";
import { defaultError, defaultSuccess } from "../common/actions";

import {
  LOAD_CONFIG_START,
  LOAD_CONFIG_ERROR,
  LOAD_CONFIG_SUCCESS
} from "./actionTypes";

export const startup = datasetId => loadDatasets(datasetId);

// TODO: Clean this up. Keeping it until expand previous queries is fixed
//
// export const startupOnQuery = (datasetId, queryId) => {
//   return dispatch =>
//     dispatch(loadDatasets(datasetId)).then(concepts => {
//       if (!concepts) return;

//       return api.getStoredQuery(datasetId, queryId).then(
//         storedQuery => {
//           dispatch(expandPreviousQuery(concepts, storedQuery.query.groups));
//           dispatch(
//             loadAllPreviousQueriesInGroups(storedQuery.query.groups, datasetId)
//           );
//         },
//         e => dispatch(selectDatasetInput(datasetId))
//       );
//     });
// };

export const loadConfigStart = () => ({ type: LOAD_CONFIG_START });
export const loadConfigError = (err: any) =>
  defaultError(LOAD_CONFIG_ERROR, err);
export const loadConfigSuccess = (res: any) =>
  defaultSuccess(LOAD_CONFIG_SUCCESS, res);

export const loadConfig = () => {
  return (dispatch: Dispatch) => {
    dispatch(loadConfigStart());

    return api
      .getFrontendConfig()
      .then(
        r => dispatch(loadConfigSuccess(r)),
        e => dispatch(loadConfigError(e))
      );
  };
};
