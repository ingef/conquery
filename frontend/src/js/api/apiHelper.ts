// Functions in this file help to transform data from our state into a shape
// that is compatible with the backend api
//
// Mainly, certain keys are allowlisted
// (to exclude others that are relevant to the frontend only)
// Some keys are added (e.g. the query type attribute)
import { isEmpty } from "../common/helpers";
import { exists } from "../common/helpers/exists";
import { isLabelPristine } from "../standard-query-editor/helper";
import type {
  TableWithFilterValueT,
  SelectedSelectorT,
  SelectedDateColumnT,
} from "../standard-query-editor/types";
import type { ValidatedTimebasedConditionT } from "../timebased-query-editor/reducer";

export const transformFilterValueToApi = (filter: any) => {
  const { value, mode } = filter;

  if (value instanceof Array) {
    return value.map((v) => (v.value ? v.value : v));
  }

  if (!!mode) {
    return mode === "range" ? value : { min: value.exact, max: value.exact };
  }

  return value;
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

export const transformElementsToApi = (conceptGroup: any) =>
  conceptGroup.map(createConcept);

const transformStandardQueryToApi = (
  query: any,
  selectedSecondaryId?: string | null,
) => {
  const queryAnd = createAnd(createQueryConcepts(query));

  return selectedSecondaryId
    ? createSecondaryIdQuery(queryAnd, selectedSecondaryId)
    : createConceptQuery(queryAnd);
};

const createSecondaryIdQuery = (root: any, secondaryId: string) => ({
  type: "SECONDARY_ID_QUERY",
  secondaryId,
  root,
});

const createConceptQuery = (root: any) => ({
  type: "CONCEPT_QUERY",
  root,
});

const createAnd = (children: any) => ({
  type: "AND",
  children,
});

const createNegation = (group: any) => ({
  type: "NEGATION",
  child: group,
});

const createDateRestriction = (dateRange: any, concept: any) => ({
  type: "DATE_RESTRICTION",
  dateRange: dateRange,
  child: concept,
});

const createSavedQuery = (conceptId: any) => ({
  type: "SAVED_QUERY",
  query: conceptId,
});

const createQueryConcept = (concept: any) =>
  concept.isPreviousQuery
    ? createSavedQuery(concept.id)
    : createConcept(concept);

const createConcept = (concept: any) => ({
  type: "CONCEPT",
  ids: concept.ids,
  label: isLabelPristine(concept) ? undefined : concept.label,
  excludeFromTimeAggregation: concept.excludeTimestamps,
  excludeFromSecondaryIdQuery: concept.excludeFromSecondaryIdQuery,
  tables: transformTablesToApi(concept.tables),
  selects: transformSelectsToApi(concept.selects),
});

const createQueryConcepts = (query: any) => {
  return query.map((group: any) => {
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

const transformTimebasedQueryToApi = (query: any) =>
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

const transformExternalQueryToApi = (query: any) =>
  createConceptQuery(createExternal(query));

const createExternal = (query: any) => {
  return {
    type: "EXTERNAL",
    format: query.format,
    values: query.values,
  };
};

// The query state already contains the query.
// But small additions are made (properties allowlisted), empty things filtered out
// to make it compatible with the backend API
export const transformQueryToApi = (
  query: Object,
  options: { queryType: string; selectedSecondaryId?: string | null },
) => {
  switch (options.queryType) {
    case "timebased":
      return transformTimebasedQueryToApi(query);
    case "standard":
      return transformStandardQueryToApi(query, options.selectedSecondaryId);
    case "external":
      return transformExternalQueryToApi(query);
    default:
      return null;
  }
};
