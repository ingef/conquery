// @flow

import { type Dispatch } from "redux-thunk";

import { type DatasetIdType } from "../dataset/reducer";
import api from "../api";
import { defaultSuccess, defaultError } from "../common/actions";
import type { ConceptIdT } from "../api/types";
import { isEmpty } from "../common/helpers";

import { resetAllTrees, search } from "./globalTreeStoreHelper";
import {
  LOAD_TREES_START,
  LOAD_TREES_SUCCESS,
  LOAD_TREES_ERROR,
  LOAD_TREE_START,
  LOAD_TREE_SUCCESS,
  LOAD_TREE_ERROR,
  CLEAR_TREES,
  SEARCH_TREES_START,
  SEARCH_TREES_SUCCESS,
  SEARCH_TREES_ERROR,
  CHANGE_SEARCH_QUERY,
  CLEAR_SEARCH_QUERY,
  TOGGLE_SHOW_MISMATCHES
} from "./actionTypes";

export const clearTrees = () => ({ type: CLEAR_TREES });

export const loadTreesStart = () => ({ type: LOAD_TREES_START });
export const loadTreesError = (err: any) => defaultError(LOAD_TREES_ERROR, err);
export const loadTreesSuccess = (res: any) =>
  defaultSuccess(LOAD_TREES_SUCCESS, res);

export const loadTrees = (datasetId: DatasetIdType) => {
  return (dispatch: Dispatch) => {
    // TODO: Careful, side effect, extract this soon
    resetAllTrees();

    dispatch(clearTrees());
    dispatch(loadTreesStart());

    return api.getConcepts(datasetId).then(
      r => {
        dispatch(loadTreesSuccess(r));

        if (!r.concepts) return;

        // Assign default select filter values
        for (const concept of Object.values(r.concepts))
          for (const table of concept.tables || [])
            for (const filter of table.filters || [])
              if (filter.defaultValue) filter.value = filter.defaultValue;

        Object.keys(r.concepts).forEach(conceptId => {
          if (r.concepts[conceptId].detailsAvailable)
            dispatch(loadTree(datasetId, conceptId));
        });

        return r.concepts;
      },
      e => dispatch(loadTreesError(e))
    );
  };
};

export const loadTreeStart = (treeId: ConceptIdT) => ({
  type: LOAD_TREE_START,
  payload: { treeId }
});
export const loadTreeError = (treeId: ConceptIdT, err: any) =>
  defaultError(LOAD_TREE_ERROR, err, { treeId });
export const loadTreeSuccess = (treeId: ConceptIdT, res: any) =>
  defaultSuccess(LOAD_TREE_SUCCESS, res, { treeId });

export const loadTree = (datasetId: DatasetIdType, treeId: ConceptIdT) => {
  return (dispatch: Dispatch) => {
    dispatch(loadTreeStart(treeId));

    return api
      .getConcept(datasetId, treeId)
      .then(
        r => dispatch(loadTreeSuccess(treeId, r)),
        e => dispatch(loadTreeError(treeId, e))
      );
  };
};

export const searchTreesStart = (query: string) => ({
  type: SEARCH_TREES_START,
  payload: { query }
});
export const searchTreesSuccess = (query: string, result: Object) => ({
  type: SEARCH_TREES_SUCCESS,
  payload: { query, result }
});
export const searchTreesError = (query: string, err: any) =>
  defaultError(SEARCH_TREES_ERROR, err, { query });

export const searchTrees = (datasetId: DatasetIdType, query: string) => {
  return async (dispatch: Dispatch) => {
    dispatch(searchTreesStart(query));

    if (isEmpty(query)) return;

    try {
      const result = await search(query);

      dispatch(searchTreesSuccess(query, result));
    } catch (e) {
      dispatch(searchTreesError(query, e));
    }
  };
};

export const clearSearchQuery = () => ({ type: CLEAR_SEARCH_QUERY });
export const changeSearchQuery = (query: string) => ({
  type: CHANGE_SEARCH_QUERY,
  payload: { query }
});

export const toggleShowMismatches = () => ({ type: TOGGLE_SHOW_MISMATCHES });
