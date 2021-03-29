import { useDispatch } from "react-redux";
import type {
  AndQueryT,
  ConceptIdT,
  DatasetIdT,
  QueryT,
  QueryNodeT,
  PostFilterSuggestionsResponseT,
} from "../api/types";

import { defaultSuccess, defaultError } from "../common/actions";
import { useLoadPreviousQuery } from "../previous-queries/list/actions";
import type { TreesT } from "../concept-trees/reducer";
import {
  PostPrefixForSuggestionsParams,
  usePostPrefixForSuggestions,
} from "../api/api";
import type { ModeT } from "../form-components/InputRange";

import type { DraggedNodeType, DraggedQueryType } from "./types";
import {
  DROP_AND_NODE,
  DROP_OR_NODE,
  DELETE_NODE,
  DELETE_GROUP,
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  CLEAR_QUERY,
  EXPAND_PREVIOUS_QUERY,
  SELECT_NODE_FOR_EDITING,
  DESELECT_NODE,
  UPDATE_NODE_LABEL,
  ADD_CONCEPT_TO_NODE,
  REMOVE_CONCEPT_FROM_NODE,
  TOGGLE_TABLE,
  SET_FILTER_VALUE,
  SET_SELECTS,
  SET_TABLE_SELECTS,
  RESET_ALL_FILTERS,
  SWITCH_FILTER_MODE,
  TOGGLE_TIMESTAMPS,
  LOAD_FILTER_SUGGESTIONS_START,
  LOAD_FILTER_SUGGESTIONS_SUCCESS,
  LOAD_FILTER_SUGGESTIONS_ERROR,
  SET_DATE_COLUMN,
  SET_SELECTED_SECONDARY_ID,
  TOGGLE_SECONDARY_ID_EXCLUDE,
} from "./actionTypes";
import { StandardQueryStateT } from "./queryReducer";
import { useTranslation } from "react-i18next";

export const dropAndNode = (item: DraggedNodeType | DraggedQueryType) => ({
  type: DROP_AND_NODE,
  payload: { item },
});

export const dropOrNode = (
  item: DraggedNodeType | DraggedQueryType,
  andIdx: number
) => ({
  type: DROP_OR_NODE,
  payload: { item, andIdx },
});

export const deleteNode = (andIdx: number, orIdx: number) => ({
  type: DELETE_NODE,
  payload: { andIdx, orIdx },
});

export const deleteGroup = (andIdx: number) => ({
  type: DELETE_GROUP,
  payload: { andIdx },
});

export const toggleExcludeGroup = (andIdx: number) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx },
});

export const loadQuery = (query: StandardQueryStateT) => ({
  type: LOAD_QUERY,
  payload: { query },
});

export const clearQuery = () => ({ type: CLEAR_QUERY });

const findPreviousQueryIds = (node: QueryNodeT, queries = []): string[] => {
  switch (node.type) {
    case "SAVED_QUERY":
      return [...queries, node.query];
    case "NEGATION":
    case "DATE_RESTRICTION":
      return findPreviousQueryIds(node.child, queries);
    case "AND":
    case "OR":
      return [
        ...queries,
        ...node.children.flatMap((child: any) =>
          findPreviousQueryIds(child, [])
        ),
      ];
    default:
      return queries;
  }
};

const isAndQuery = (query: QueryT): query is AndQueryT => {
  return query.root.type === "AND";
};
/*
  1) Expands previous query in the editor
  2) Triggers a load for all nested queries
*/
export const useExpandPreviousQuery = () => {
  const dispatch = useDispatch();
  const loadPreviousQuery = useLoadPreviousQuery();
  const { t } = useTranslation();

  return async (datasetId: DatasetIdT, rootConcepts: TreesT, query: QueryT) => {
    if (!isAndQuery(query)) {
      throw new Error("Cant expand query, because root is not AND");
    }

    const nestedPreviousQueryIds = findPreviousQueryIds(query.root);

    dispatch({
      type: EXPAND_PREVIOUS_QUERY,
      payload: {
        rootConcepts,
        query,
        expandErrorMessage: t("queryEditor.couldNotExpandNode"),
      },
    });

    await Promise.all(
      nestedPreviousQueryIds.map((queryId) =>
        loadPreviousQuery(datasetId, queryId)
      )
    );

    dispatch(
      setSelectedSecondaryId(query.secondaryId ? query.secondaryId : null)
    );
  };
};

export const selectNodeForEditing = (andIdx: number, orIdx: number) => ({
  type: SELECT_NODE_FOR_EDITING,
  payload: { andIdx, orIdx },
});

export const deselectNode = () => ({ type: DESELECT_NODE });

export const updateNodeLabel = (label: string) => ({
  type: UPDATE_NODE_LABEL,
  payload: { label },
});
export const addConceptToNode = (concept) => ({
  type: ADD_CONCEPT_TO_NODE,
  payload: { concept },
});
export const removeConceptFromNode = (conceptId: ConceptIdT) => ({
  type: REMOVE_CONCEPT_FROM_NODE,
  payload: { conceptId },
});

export const toggleTable = (tableIdx: number, isExcluded: boolean) => ({
  type: TOGGLE_TABLE,
  payload: { tableIdx, isExcluded },
});

export const setFilterValue = (
  tableIdx: number,
  filterIdx: number,
  value: unknown
) => ({
  type: SET_FILTER_VALUE,
  payload: { tableIdx, filterIdx, value },
});

export const setTableSelects = (tableIdx: number, value: unknown) => ({
  type: SET_TABLE_SELECTS,
  payload: { tableIdx, value },
});
export const setSelects = (value) => ({
  type: SET_SELECTS,
  payload: { value },
});

export const setDateColumn = (tableIdx: number, value) => ({
  type: SET_DATE_COLUMN,
  payload: { tableIdx, value },
});

export const resetAllFilters = (andIdx: number, orIdx: number) => ({
  type: RESET_ALL_FILTERS,
  payload: { andIdx, orIdx },
});

export const switchFilterMode = (
  tableIdx: number,
  filterIdx: number,
  mode: ModeT
) => ({
  type: SWITCH_FILTER_MODE,
  payload: { tableIdx, filterIdx, mode },
});

export const toggleTimestamps = (andIdx?: number, orIdx?: number) => ({
  type: TOGGLE_TIMESTAMPS,
  payload: { andIdx, orIdx },
});

export const toggleSecondaryIdExclude = (andIdx?: number, orIdx?: number) => ({
  type: TOGGLE_SECONDARY_ID_EXCLUDE,
  payload: { andIdx, orIdx },
});

export const loadFilterSuggestionsStart = (
  tableIdx: number,
  filterIdx: number
) => ({
  type: LOAD_FILTER_SUGGESTIONS_START,
  payload: { tableIdx, filterIdx },
});

export const loadFilterSuggestionsSuccess = (
  suggestions: PostFilterSuggestionsResponseT,
  tableIdx: number,
  filterIdx: number
) =>
  defaultSuccess(LOAD_FILTER_SUGGESTIONS_SUCCESS, suggestions, {
    tableIdx,
    filterIdx,
  });

export const loadFilterSuggestionsError = (
  error: Error,
  tableIdx: number,
  filterIdx: number
) =>
  defaultError(LOAD_FILTER_SUGGESTIONS_ERROR, error, { tableIdx, filterIdx });

export const useLoadFilterSuggestions = () => {
  const dispatch = useDispatch();
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  return (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number
  ) => {
    dispatch(loadFilterSuggestionsStart(tableIdx, filterIdx));

    return postPrefixForSuggestions(params).then(
      (r) => dispatch(loadFilterSuggestionsSuccess(r, tableIdx, filterIdx)),
      (e) => dispatch(loadFilterSuggestionsError(e, tableIdx, filterIdx))
    );
  };
};

export const setSelectedSecondaryId = (secondaryId: string | null) => {
  return {
    type: SET_SELECTED_SECONDARY_ID,
    payload: { secondaryId },
  };
};
