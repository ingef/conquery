// @flow

import { type Dispatch }      from 'redux-thunk';
import { type DatasetIdType } from '../dataset/reducer';

import api                    from '../api';

import {
  defaultSuccess,
  defaultError,
} from '../common/actions';

import {
  resetAllTrees
} from './globalTreeStoreHelper';

import {
  LOAD_TREES_START,
  LOAD_TREES_SUCCESS,
  LOAD_TREES_ERROR,
  LOAD_TREE_START,
  LOAD_TREE_SUCCESS,
  LOAD_TREE_ERROR,
  CLEAR_TREES
} from './actionTypes';

import { type TreeNodeIdType }    from './reducer';

export const clearTrees = () => ({ type: CLEAR_TREES });

export const loadTreesStart = () => ({ type: LOAD_TREES_START });
export const loadTreesError = (err: any) => defaultError(LOAD_TREES_ERROR, err);
export const loadTreesSuccess = (res: any) => defaultSuccess(LOAD_TREES_SUCCESS, res);

export const loadTrees = (datasetId: DatasetIdType) => {
  return (dispatch: Dispatch) => {
    // TODO: Careful, side effect, extract this soon
    resetAllTrees();

    dispatch(clearTrees());
    dispatch(loadTreesStart());

    return api.getConcepts(datasetId)
      .then(
        r => {
          dispatch(loadTreesSuccess(r));

          // Assign default select filter values
          for (const concept of Object.values(r.concepts))
            for (const table of concept.tables || [])
              for (const filter of table.filters || [])
                if (filter.defaultValue)
                  filter.value = filter.defaultValue;

          // In the future: Data could be cached, version could be checked and
          // further data only loaded when necessary
          if (r.version > -1) {
            Object
              .keys(r.concepts)
              .forEach(conceptId => {
                if (r.concepts[conceptId].detailsAvailable)
                  dispatch(loadTree(datasetId, conceptId));
              });

            return r.concepts;
          }
        },
        e => dispatch(loadTreesError(e))
      );
  };
};

export const loadTreeStart = (treeId: TreeNodeIdType) =>
  ({ type: LOAD_TREE_START, payload: { treeId } });
export const loadTreeError = (treeId: TreeNodeIdType, err: any) =>
  defaultError(LOAD_TREE_ERROR, err, { treeId });
export const loadTreeSuccess = (treeId: TreeNodeIdType, res: any) =>
  defaultSuccess(LOAD_TREE_SUCCESS, res, { treeId });

export const loadTree = (datasetId: DatasetIdType, treeId: TreeNodeIdType) => {
  return (dispatch: Dispatch) => {
    dispatch(loadTreeStart(treeId));

    return api.getConcept(datasetId, treeId)
      .then(
        r => dispatch(loadTreeSuccess(treeId, r)),
        e => dispatch(loadTreeError(treeId, e))
      );
  };
};
