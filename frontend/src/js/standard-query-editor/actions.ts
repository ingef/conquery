import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { ActionType, createAction } from "typesafe-actions";

import {
  PostPrefixForSuggestionsParams,
  usePostFilterValuesResolve,
  usePostPrefixForSuggestions,
} from "../api/api";
import type {
  AndQueryT,
  ConceptIdT,
  QueryT,
  QueryNodeT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
} from "../api/types";
import { successPayload } from "../common/actions/genericActions";
import type { TreesT } from "../concept-trees/reducer";
import { nodeIsConceptQueryNode, NodeResetConfig } from "../model/node";
import { useLoadQuery } from "../previous-queries/list/actions";
import type { ModeT } from "../ui-components/InputRange";

import { expandNode } from "./expandNode";
import { StandardQueryStateT } from "./queryReducer";
import type {
  DragItemConceptTreeNode,
  DragItemQuery,
  FilterWithValueType,
} from "./types";

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
        ...node.children.flatMap((child) => findPreviousQueryIds(child, [])),
      ];
    default:
      return queries;
  }
};

// Completely override all groups in the editor with the previous groups, but
// a) merge elements with concept data from concept trees (esp. "tables")
// b) load nested previous queries contained in that query,
//    so they can also be expanded
const createExpandedQueryState = ({
  rootConcepts,
  query,
  expandErrorMessage,
}: {
  rootConcepts: TreesT;
  query: AndQueryT;
  expandErrorMessage: string;
}): StandardQueryStateT => {
  return query.root.children.map((child) =>
    expandNode(rootConcepts, child, expandErrorMessage),
  );
};

export const expandPreviousQuery = createAction(
  "query-editor/EXPAND_PREVIOUS_QUERY",
)<StandardQueryStateT>();

const useLoadBigMultiSelectValues = () => {
  const postFilterValuesResolve = usePostFilterValuesResolve();

  return useCallback(
    // Actually, state is a StandardQueryStateT
    // where all big multi select filters
    // don't have value: SelectOptionT[] yet, but string[]
    // we just don't have an extra type for it.
    async (state: StandardQueryStateT): Promise<StandardQueryStateT> => {
      return Promise.all(
        state.map(async (val) => ({
          ...val,
          elements: await Promise.all(
            val.elements.map(async (el) => {
              if (!nodeIsConceptQueryNode(el)) return el;
              return {
                ...el,
                tables: await Promise.all(
                  el.tables.map(async (table) => ({
                    ...table,
                    filters: await Promise.all(
                      table.filters.map(async (filter) => {
                        if (
                          filter.type !== "BIG_MULTI_SELECT" ||
                          !filter.value ||
                          filter.value.length === 0
                        ) {
                          return filter;
                        }

                        try {
                          const result = await postFilterValuesResolve(
                            filter.id,
                            filter.value as unknown as string[], // See explanation above
                          );
                          return {
                            ...filter,
                            value: result.resolvedFilter?.value || [],
                          };
                        } catch (e) {
                          console.error(e);
                          return { ...filter, value: [] };
                        }
                      }),
                    ),
                  })),
                ),
              };
            }),
          ),
        })),
      );
    },
    [postFilterValuesResolve],
  );
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
  const { loadQuery } = useLoadQuery();
  const { t } = useTranslation();
  const loadBigMultiSelectValues = useLoadBigMultiSelectValues();

  return useCallback(
    async (rootConcepts: TreesT, query: QueryT) => {
      if (!isAndQuery(query)) {
        throw new Error("Cant expand query, because root is not AND");
      }

      const nestedPreviousQueryIds = findPreviousQueryIds(query.root);

      const expandedQueryState = await loadBigMultiSelectValues(
        createExpandedQueryState({
          rootConcepts,
          query,
          expandErrorMessage: t("queryEditor.couldNotExpandNode"),
        }),
      );

      dispatch(expandPreviousQuery(expandedQueryState));

      await Promise.all(
        nestedPreviousQueryIds.map((queryId) => loadQuery(queryId)),
      );

      dispatch(
        setSelectedSecondaryId({
          secondaryId: query.secondaryId ? query.secondaryId : null,
        }),
      );
    },
    [dispatch, t, loadQuery, loadBigMultiSelectValues],
  );
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
  value: FilterWithValueType["value"]; // Actually: FilterWithValueType["value"] which is overloaded;
}>();

export const setTableSelects = createAction("query-editor/SET_TABLE_SELECTS")<{
  andIdx: number;
  orIdx: number;
  tableIdx: number;
  value: SelectOptionT[];
}>();
export const setSelects = createAction("query-editor/SET_SELECTS")<{
  andIdx: number;
  orIdx: number;
  value: SelectOptionT[];
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
  page: number;
}
export const loadFilterSuggestionsSuccess = createAction(
  "query-editor/LOAD_FILTER_SUGGESTIONS_SUCCESS",
)<
  FilterContext & {
    data: PostFilterSuggestionsResponseT;
  }
>();

export const useLoadFilterSuggestions = (
  editedNode: { andIdx: number; orIdx: number } | null,
) => {
  const dispatch = useDispatch();
  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  return useCallback(
    async (
      params: PostPrefixForSuggestionsParams,
      tableIdx: number,
      filterIdx: number,
      { returnOnly }: { returnOnly?: boolean } = {},
    ) => {
      if (!editedNode) return null;

      const context = { ...editedNode, tableIdx, filterIdx, page: params.page };

      const suggestions = await postPrefixForSuggestions(params);

      if (!returnOnly) {
        dispatch(
          loadFilterSuggestionsSuccess(successPayload(suggestions, context)),
        );
      }

      return suggestions;
    },
    [dispatch, editedNode, postPrefixForSuggestions],
  );
};

export const setSelectedSecondaryId = createAction(
  "query-editor/SET_SELECTED_SECONDARY_ID",
)<{ secondaryId: string | null }>();
