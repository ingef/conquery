// Functions in this file help to transform data from our state into a shape
// that is compatible with the backend api
//
// Mainly, certain keys are allowlisted
// (to exclude others that are relevant to the frontend only)
// Some keys are added (e.g. the query type attribute)
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import { EditorV2Query, Tree } from "../editor-v2/types";
import { nodeIsConceptQueryNode } from "../model/node";
import { isLabelPristine } from "../standard-query-editor/helper";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import type {
  TableWithFilterValueT,
  SelectedSelectorT,
  SelectedDateColumnT,
  StandardQueryNodeT,
  DragItemConceptTreeNode,
  FilterWithValueType,
} from "../standard-query-editor/types";
import type {
  ValidatedTimebasedConditionT,
  ValidatedTimebasedQueryStateT,
} from "../timebased-query-editor/reducer";

import { ConceptIdT, DateRangeT } from "./types";

export const transformFilterValueToApi = (
  filter: FilterWithValueType,
): {} | null /* aka: "not undefined", to ensure a type error when a new filter.type is added */ => {
  switch (filter.type) {
    case "BIG_MULTI_SELECT":
    case "MULTI_SELECT":
      return filter.value
        ? filter.value.filter((v) => exists(v.value)).map((v) => v.value)
        : null;
    case "INTEGER_RANGE":
    case "MONEY_RANGE":
    case "REAL_RANGE":
      return !exists(filter.mode) || filter.mode === "range"
        ? filter.value
        : filter.value
        ? { min: filter.value.exact, max: filter.value.exact }
        : null;
    case "SELECT":
      return filter.value;
  }
};

export const transformSelectsToApi = (selects?: SelectedSelectorT[] | null) => {
  if (!selects) return [];

  return selects
    ? selects.filter(({ selected }) => !!selected).map(({ id }) => id)
    : [];
};

export const transformDateColumnToApi = (dateColumn?: SelectedDateColumnT) => {
  if (!dateColumn) return null;

  return {
    value: dateColumn.value,
  };
};

export const transformTablesToApi = (tables: TableWithFilterValueT[]) => {
  if (!tables) return [];

  return tables
    .filter((table) => !table.exclude)
    .map((table) => {
      // Explicitly allowlist the tables that we allow to send to the API
      return {
        id: table.connectorId,
        dateColumn: transformDateColumnToApi(table.dateColumn),
        selects: transformSelectsToApi(table.selects),
        filters: table.filters
          ? table.filters
              .filter(
                (filter) => exists(filter.value) && !isEmpty(filter.value),
              ) // Only send filters with a value
              .map((filter) => ({
                filter: filter.id,
                type: filter.type,
                value: transformFilterValueToApi(filter),
              }))
          : [],
      };
    });
};

export const transformElementsToApi = (
  conceptGroup: DragItemConceptTreeNode[],
) => conceptGroup.map(createConcept);

const transformStandardQueryToApi = (
  query: StandardQueryStateT,
  selectedSecondaryId?: string | null,
) => {
  const queryAnd = createAnd(createQueryConcepts(query));

  return selectedSecondaryId
    ? createSecondaryIdQuery(queryAnd, selectedSecondaryId)
    : createConceptQuery(queryAnd);
};

const createSecondaryIdQuery = <T>(root: T, secondaryId: string) => ({
  type: "SECONDARY_ID_QUERY" as const,
  secondaryId,
  root,
});

const createConceptQuery = <T>(root: T) => ({
  type: "CONCEPT_QUERY" as const,
  root,
});

const createAnd = <T>(children: T) => ({
  type: "AND" as const,
  children,
});

const createNegation = <T>(group: T) => ({
  type: "NEGATION" as const,
  child: group,
});

const createDateRestriction = <T>(dateRange: DateRangeT, concept: T) => ({
  type: "DATE_RESTRICTION" as const,
  dateRange: dateRange,
  child: concept,
});

const createSavedQuery = (conceptId: ConceptIdT) => ({
  type: "SAVED_QUERY" as const,
  query: conceptId,
});

const createQueryConcept = (concept: StandardQueryNodeT) =>
  nodeIsConceptQueryNode(concept)
    ? createConcept(concept)
    : createSavedQuery(concept.id);

const createConcept = (concept: DragItemConceptTreeNode) => ({
  type: "CONCEPT" as const,
  ids: concept.ids,
  label: isLabelPristine(concept) ? undefined : concept.label,
  excludeFromTimeAggregation: concept.excludeTimestamps,
  excludeFromSecondaryId: concept.excludeFromSecondaryId,
  tables: transformTablesToApi(concept.tables),
  selects: transformSelectsToApi(concept.selects),
});

const createQueryConcepts = (query: StandardQueryStateT) => {
  return query.map((group) => {
    const concepts = group.elements.map(createQueryConcept);
    const orConcept = { type: "OR", children: [...concepts] };

    const withDate = group.dateRange
      ? createDateRestriction(group.dateRange, orConcept)
      : orConcept;

    return group.exclude ? createNegation(withDate) : withDate;
  });
};

// TODO: Use, once feature is complete
const getDays = (condition: ValidatedTimebasedConditionT) => {
  switch (condition.operator) {
    case "DAYS_BEFORE":
      return {
        days: {
          min: condition.minDays,
          max: condition.maxDays,
        },
      };
    case "DAYS_OR_NO_EVENT_BEFORE":
      return {
        days: condition.minDaysOrNoEvent,
      };
    default:
      return {};
  }
};

const transformTimebasedQueryToApi = (query: ValidatedTimebasedQueryStateT) =>
  createConceptQuery(
    createAnd(
      query.conditions.map((condition: ValidatedTimebasedConditionT) => {
        const days = getDays(condition);

        return {
          type: condition.operator,
          ...days,
          preceding: {
            sampler: condition.result0.timestamp,
            child: createSavedQuery(condition.result0.id),
          },
          index: {
            sampler: condition.result1.timestamp,
            child: createSavedQuery(condition.result1.id),
          },
        };
      }),
    ),
  );

const transformTreeToApi = (tree: Tree) => {
  let dateRestriction;
  if (tree.dates?.restriction) {
    dateRestriction = createDateRestriction(tree.dates.restriction, tree);
  }

  let negation;
  if (tree.negation) {
    negation = createNegation(tree);
  }

  let combined;
  if (dateRestriction && negation) {
    combined = dateRestriction;
    combined.child = negation;
  } else if (dateRestriction) {
    combined = dateRestriction;
  } else if (negation) {
    combined = negation;
  } else {
    combined = tree;

  tree.dates;
};

const transformEditorV2QueryToApi = (query: EditorV2Query) => {
  if (!query.tree) return null;

  return transformTreeToApi(query.tree);
};

// The query state already contains the query.
// But small additions are made (properties allowlisted), empty things filtered out
// to make it compatible with the backend API
export const transformQueryToApi = (
  query: StandardQueryStateT | ValidatedTimebasedQueryStateT | EditorV2Query,
  options: { queryType: string; selectedSecondaryId?: string | null },
) => {
  switch (options.queryType) {
    case "timebased":
      return transformTimebasedQueryToApi(
        query as ValidatedTimebasedQueryStateT,
      );
    case "standard":
      return transformStandardQueryToApi(
        query as StandardQueryStateT,
        options.selectedSecondaryId,
      );
    case "editorV2":
      return transformEditorV2QueryToApi(query as EditorV2Query);
    default:
      return null;
  }
};
