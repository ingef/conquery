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
import { successPayload } from "../common/actions";
import type { TreesT } from "../concept-trees/reducer";
import type { NodeResetConfig } from "../model/node";
import { useLoadQuery } from "../previous-queries/list/actions";
import type { ModeT } from "../ui-components/InputRange";

import { StandardQueryStateT } from "./queryReducer";
import type { DragItemConceptTreeNode, DragItemQuery } from "./types";

export type StandardQueryEditorActions = ActionType<
  | typeof resetTable
  | typeof dropAndNode
  | typeof dropOrNode
  | typeof loadSavedQuery
  | typeof clearQuery
  | typeof deleteNode
  | typeof deleteGroup
  | typeof updateNodeLabel
  | typeof toggleTable
  | typeof setFilterValue
  | typeof toggleExcludeGroup
  | typeof toggleSecondaryIdExclude
  | typeof toggleTimestamps
  | typeof resetAllSettings
  | typeof removeConceptFromNode
  | typeof addConceptToNode
  | typeof switchFilterMode
  | typeof setSelects
  | typeof setTableSelects
  | typeof setDateColumn
  | typeof setSelectedSecondaryId
  | typeof expandPreviousQuery
  | typeof loadFilterSuggestionsSuccess
>;

export const dropAndNode = createAction("query-editor/DROP_AND_NODE")<{
  item: DragItemConceptTreeNode | DragItemQuery;
}>();

export const dropOrNode = createAction("query-editor/DROP_OR_NODE")<{
  item: DragItemConceptTreeNode | DragItemQuery;
  andIdx: number;
}>();

export const deleteNode = createAction("query-editor/DELETE_NODE")<{
  andIdx: number;
  orIdx: number;
}>();

export const deleteGroup = createAction("query-editor/DELETE_GROUP")<{
  andIdx: number;
}>();

export const toggleExcludeGroup = createAction(
  "query-editor/TOGGLE_EXCLUDE_GROUP",
)<{ andIdx: number }>();

export const loadSavedQuery = createAction("query-editor/LOAD_SAVED_QUERY")<{
  query: StandardQueryStateT;
}>();

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

export const expandPreviousQuery = createAction(
  "query-editor/EXPAND_PREVIOUS_QUERY",
)<{
  rootConcepts: TreesT;
  query: AndQueryT;
  expandErrorMessage: string;
}>();

const isAndQuery = (query: QueryT): query is AndQueryT => {
  return query.root.type === "AND";
};
/*
  1) Expands previous query in the editor
  2) Triggers a load for all nested queries
*/
export const useExpandPreviousQuery = () => {
  const dispatch = useDispatch();
  const loadQuery = useLoadQuery();
  const { t } = useTranslation();

  return async (datasetId: DatasetIdT, rootConcepts: TreesT, query: QueryT) => {
    if (!isAndQuery(query)) {
      throw new Error("Cant expand query, because root is not AND");
    }

    const nestedPreviousQueryIds = findPreviousQueryIds(query.root);

    dispatch(
      expandPreviousQuery({
        rootConcepts,
        query,
        expandErrorMessage: t("queryEditor.couldNotExpandNode"),
      }),
    );

    await Promise.all(
      nestedPreviousQueryIds.map((queryId) => loadQuery(datasetId, queryId)),
    );

    dispatch(
      setSelectedSecondaryId({
        secondaryId: query.secondaryId ? query.secondaryId : null,
      }),
    );
  };
};

export const updateNodeLabel = createAction("query-editor/UPDATE_NODE_LABEL")<{
  andIdx: number;
  orIdx: number;
  label: string;
}>();

export const addConceptToNode = createAction(
  "query-editor/ADD_CONCEPT_TO_NODE",
)<{
  andIdx: number;
  orIdx: number;
  concept: DragItemConceptTreeNode;
}>();

export const removeConceptFromNode = createAction(
  "query-editor/REMOVE_CONCEPT_FROM_NODE",
)<{ andIdx: number; orIdx: number; conceptId: ConceptIdT }>();

export const toggleTable = createAction("query-editor/TOGGLE_TABLE")<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  isExcluded: boolean;
}>();

export const setFilterValue = createAction("query-editor/SET_FILTER_VALUE")<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  filterIdx: number;
  value: unknown;
}>();

export const setTableSelects = createAction("query-editor/SET_TABLE_SELECTS")<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  value: unknown;
}>();
export const setSelects = createAction("query-editor/SET_SELECTS")<{
  andIdx: number;
  orIdx: number;
  value: unknown;
}>();
export const setDateColumn = createAction("query-editor/SET_DATE_COLUMN")<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  value: string;
}>();

export const resetAllSettings = createAction(
  "query-editor/RESET_ALL_SETTINGS",
)<{
  andIdx: number;
  orIdx: number;
  config: NodeResetConfig;
}>();

export const resetTable = createAction("query-editor/RESET_TABLE")<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  config: NodeResetConfig;
}>();

export const switchFilterMode = createAction(
  "query-editor/SWITCH_FILTER_MODE",
)<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  filterIdx: number;
  mode: ModeT;
}>();

export const toggleTimestamps = createAction("query-editor/TOGGLE_TIMESTAMPS")<{
  andIdx: number;
  orIdx: number;
}>();

export const toggleSecondaryIdExclude = createAction(
  "query-editor/TOGGLE_SECONDARY_ID_EXCLUDE",
)<{ andIdx: number; orIdx: number }>();

interface FilterContext {
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  filterIdx: number;
}
export const loadFilterSuggestionsSuccess = createAction(
  "query-editor/LOAD_FILTER_SUGGESTIONS_SUCCESS",
)<
  FilterContext & {
    data: PostFilterSuggestionsResponseT["values"];
  }
>();

export const useLoadFilterSuggestions = (
  editedNode: { andIdx: number; orIdx: number } | null,
) => {
  const dispatch = useDispatch();
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  return async (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
  ) => {
    if (!editedNode) return;

    const context = { ...editedNode, tableIdx, filterIdx };

    const suggestions = await postPrefixForSuggestions(params);

    dispatch(
      loadFilterSuggestionsSuccess(successPayload(suggestions.values, context)),
    );
  };
};

export const setSelectedSecondaryId = createAction(
  "query-editor/SET_SELECTED_SECONDARY_ID",
)<{ secondaryId: string | null }>();
