// @flow

// Functions in this file help to transform data from our state into a shape
// that is compatible with the backend api
//
// Mainly, certain keys are whitelisted
// (to exclude others that are relevant to the frontend only)
// Some keys are added (e.g. the query type attribute)

import {
  DAYS_BEFORE,
  DAYS_OR_NO_EVENT_BEFORE
} from "../common/constants/timebasedQueryOperatorTypes";

import { isEmpty } from "../common/helpers";

import type {
  TableWithFilterValueType,
  SelectedSelectorType
} from "../standard-query-editor/types";

export const transformFilterValueToApi = (filter: any) => {
  const { value, mode } = filter;

  if (value instanceof Array) {
    return value.map(v => (v.value ? v.value : v));
  }

  if (!!mode) {
    return mode === "range" ? value : { min: value.exact, max: value.exact };
  }

  return value;
};

export const transformSelectsToApi = (selects?: ?(SelectedSelectorType[])) => {
  if (!selects) return [];

  return selects
    ? selects.filter(({ selected }) => !!selected).map(({ id }) => id)
    : [];
};

export const transformTablesToApi = (tables: TableWithFilterValueType[]) => {
  if (!tables) return [];

  return tables
    .filter(table => !table.exclude)
    .map(table => {
      // Explicitly whitelist the tables that we allow to send to the API
      return {
        id: table.connectorId,
        selects: transformSelectsToApi(table.selects),
        filters: table.filters
          ? table.filters
              .filter(filter => !isEmpty(filter.value)) // Only send filters with a value
              .map(filter => ({
                filter: filter.id,
                type: filter.type,
                value: transformFilterValueToApi(filter)
              }))
          : []
      };
    });
};

export const transformElementGroupsToApi = elementGroups =>
  elementGroups.map(elements => ({
    matchingType: elements.matchingType,
    type: "OR",
    children: transformElementsToApi(elements.concepts)
  }));

export const transformElementsToApi = conceptGroup =>
  conceptGroup.map(createConcept);

const transformStandardQueryToApi = query =>
  createConceptQuery(createQueryConcepts(query));

const createConceptQuery = children => ({
  type: "CONCEPT_QUERY",
  root: {
    type: "AND",
    children: children
  }
});

const createNegation = group => ({
  type: "NEGATION",
  child: group
});

const createDateRestriction = (dateRange, concept) => ({
  type: "DATE_RESTRICTION",
  dateRange: dateRange,
  child: concept
});

const createSavedQuery = conceptId => ({
  type: "SAVED_QUERY",
  query: conceptId
});

const createQueryConcept = concept =>
  concept.isPreviousQuery
    ? createSavedQuery(concept.id)
    : createConcept(concept);

const createConcept = concept => ({
  type: "CONCEPT",
  ids: concept.ids,
  label: concept.label,
  excludeFromTimeAggregation: concept.excludeTimestamps,
  tables: transformTablesToApi(concept.tables),
  selects: transformSelectsToApi(concept.selects)
});

const createQueryConcepts = query => {
  return query.map(group => {
    const concepts = group.elements.map(createQueryConcept);
    const orConcept = { type: "OR", children: [...concepts] };

    const withDate = group.dateRange
      ? createDateRestriction(group.dateRange, orConcept)
      : orConcept;

    return group.exclude ? createNegation(withDate) : withDate;
  });
};

// TODO: Use, once feature is complete
const getDays = condition => {
  switch (condition.operator) {
    case DAYS_BEFORE:
      return {
        days: {
          min: condition.minDays,
          max: condition.maxDays
        }
      };
    case DAYS_OR_NO_EVENT_BEFORE:
      return {
        days: condition.minDaysOrNoEvent
      };
    default:
      return {};
  }
};

const transformTimebasedQueryToApi = query => ({
  type: "CONCEPT_QUERY",
  root: {
    type: "AND",
    children: query.conditions.map(condition => {
      const days = getDays(condition);

      return {
        type: condition.operator,
        ...days,
        preceding: {
          sampler: condition.result0.timestamp,
          child: createSavedQuery(condition.result0.id)
        },
        index: {
          sampler: condition.result1.timestamp,
          child: createSavedQuery(condition.result1.id)
        }
      };
    })
  }
});

const transformExternalQueryToApi = query =>
  createConceptQuery(createExternal(query));

const createExternal = (query: any) => {
  return {
    type: "EXTERNAL",
    format:
      // This is experimental still.
      // External queries (uploaded lists) may contain three or four columns.
      // The first two columns are IDs, which will be concatenated
      // The other two columns are date ranges
      // We simply assume that the data is in this format
      // Will produce upload errors when the data has a different format
      //
      // Based on the possible:
      // ID (some string)
      // EVENT_DATE (a single day),
      // START_DATE (a starting day),
      // END_DATE (and end day,
      // DATE_RANGE (two days),
      // DATE_SET (a set of date ranges),
      // IGNORE (ignore this column);
      query.data[0].length >= 4
        ? ["ID", "ID", "START_DATE", "END_DATE"]
        : ["ID", "ID", "DATE_SET"],
    values: query.data.slice(1)
  };
};

// The query state already contains the query.
// But small additions are made (properties whitelisted), empty things filtered out
// to make it compatible with the backend API
export const transformQueryToApi = (query: Object, queryType: string) => {
  switch (queryType) {
    case "timebased":
      return transformTimebasedQueryToApi(query);
    case "standard":
      return transformStandardQueryToApi(query);
    case "external":
      return transformExternalQueryToApi(query);
    default:
      return null;
  }
};
