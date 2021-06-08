import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { ActionType, createAction } from "typesafe-actions";

import {
  PostPrefixForSuggestionsParams,
  usePostPrefixForSuggestions,
} from "../api/api";
import type {
  AndQueryT,
  ConceptIdT,
  DatasetIdT,
  QueryT,
  QueryNodeT,
  PostFilterSuggestionsResponseT,
} from "../api/types";
import { defaultSuccess, defaultError } from "../common/actions";
import type { TreesT } from "../concept-trees/reducer";
import type { ModeT } from "../form-components/InputRange";
import { useLoadPreviousQuery } from "../previous-queries/list/actions";

import {
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  EXPAND_PREVIOUS_QUERY,
  SELECT_NODE_FOR_EDITING,
  DESELECT_NODE,
  ADD_CONCEPT_TO_NODE,
  REMOVE_CONCEPT_FROM_NODE,
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
import type {
  DragItemConceptTreeNode,
  DragItemNode,
  DragItemQuery,
} from "./types";

export type StandardQueryEditorActions = ActionType<
  | typeof resetTable
  | typeof dropAndNode
  | typeof dropOrNode
  | typeof clearQuery
  | typeof deleteNode
  | typeof deleteGroup
  | typeof updateNodeLabel
  | typeof toggleTable
  | typeof setFilterValue
>;

export const dropAndNode = createAction("query-editor/DROP_AND_NODE")<{
  item: DragItemConceptTreeNode | DragItemQuery | DragItemNode;
}>();

export const dropOrNode = createAction("query-editor/DROP_OR_NODE")<{
  item: DragItemConceptTreeNode | DragItemQuery | DragItemNode;
  andIdx: number;
}>();

export const deleteNode = createAction("query-editor/DELETE_NODE")<{
  andIdx: number;
  orIdx: number;
}>();

export const deleteGroup = createAction("query-editor/DELETE_GROUP")<{
  andIdx: number;
}>();

export const toggleExcludeGroup = (andIdx: number) => ({
  type: TOGGLE_EXCLUDE_GROUP,
  payload: { andIdx },
});

export const loadQuery = (query: StandardQueryStateT) => ({
  type: LOAD_QUERY,
  payload: { query },
});

export const clearQuery = createAction("query-editor/CLEAR_QUERY")();

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
          findPreviousQueryIds(child, []),
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
        loadPreviousQuery(datasetId, queryId),
      ),
    );

    dispatch(
      setSelectedSecondaryId(query.secondaryId ? query.secondaryId : null),
    );
  };
};

export const selectNodeForEditing = (andIdx: number, orIdx: number) => ({
  type: SELECT_NODE_FOR_EDITING,
  payload: { andIdx, orIdx },
});

export const deselectNode = () => ({ type: DESELECT_NODE });

export const updateNodeLabel = createAction("query-editor/UPDATE_NODE_LABEL")<{
  label: string;
}>();

export const addConceptToNode = (concept: DragItemConceptTreeNode) => ({
  type: ADD_CONCEPT_TO_NODE,
  payload: { concept },
});
export const removeConceptFromNode = (conceptId: ConceptIdT) => ({
  type: REMOVE_CONCEPT_FROM_NODE,
  payload: { conceptId },
});

export const toggleTable = createAction("query-editor/TOGGLE_TABLE")<{
  tableIdx: number;
  isExcluded: boolean;
}>();

export const setFilterValue = createAction("query-editor/SET_FILTER_VALUE")<{
  tableIdx: number;
  filterIdx: number;
  value: unknown;
}>();

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

export const resetAllFilters = () => ({
  type: RESET_ALL_FILTERS,
});

export const resetTable = createAction("query-editor/RESET_TABLE")<{
  tableIdx: number;
}>();

export const switchFilterMode = (
  tableIdx: number,
  filterIdx: number,
  mode: ModeT,
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
  filterIdx: number,
) => ({
  type: LOAD_FILTER_SUGGESTIONS_START,
  payload: { tableIdx, filterIdx },
});

export const loadFilterSuggestionsSuccess = (
  suggestions: PostFilterSuggestionsResponseT,
  tableIdx: number,
  filterIdx: number,
) =>
  defaultSuccess(LOAD_FILTER_SUGGESTIONS_SUCCESS, suggestions, {
    tableIdx,
    filterIdx,
  });

export const loadFilterSuggestionsError = (
  error: Error,
  tableIdx: number,
  filterIdx: number,
) =>
  defaultError(LOAD_FILTER_SUGGESTIONS_ERROR, error, { tableIdx, filterIdx });

export const useLoadFilterSuggestions = () => {
  const dispatch = useDispatch();
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  return (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
  ) => {
    dispatch(loadFilterSuggestionsStart(tableIdx, filterIdx));

    return postPrefixForSuggestions(params).then(
      (r) => dispatch(loadFilterSuggestionsSuccess(r, tableIdx, filterIdx)),
      (e) => dispatch(loadFilterSuggestionsError(e, tableIdx, filterIdx)),
    );
  };
};

export const setSelectedSecondaryId = (secondaryId: string | null) => {
  return {
    type: SET_SELECTED_SECONDARY_ID,
    payload: { secondaryId },
  };
};
