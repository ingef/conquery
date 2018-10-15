// @flow

// Functions in this file help to transform data from our state into a shape
// that is compatible with the backend api
//
// Mainly, certain keys are whitelisted
// (to exclude others that are relevant to the frontend only)
// Some keys are added (e.g. the query type attribute)

import {
  DAYS_BEFORE,
  DAYS_OR_NO_EVENT_BEFORE,
} from '../common/constants/timebasedQueryOperatorTypes';

import {
  isEmpty
} from '../common/helpers';

import { type TableType } from '../standard-query-editor/types';

export const transformTablesToApi = (tables: TableType[]) => {
  if (!tables) return [];

  return tables
    .filter(table => !table.exclude)
    .map(table => {
      // Explicitly whitelist the tables that we allow to send to the API
      return {
        id: table.id,
        filters: table.filters
          ? table.filters
            .filter(filter => !isEmpty(filter.value)) // Only send filters with a value
            .map(filter => ({
              id: filter.id,
              type: filter.type,
              value: filter.value
            }))
          : []
        };
      });
}

export const transformElementGroupsToApi = (elementGroups) => elementGroups.map(elements => ({
  matchingType: elements.matchingType,
  elements: transformElementsToApi(elements.concepts)
}));

export const transformElementsToApi = (conceptGroup) => conceptGroup.map(concept => {
  const tables = concept.tables
    ? transformTablesToApi(concept.tables)
    : [];

  return {
    ids: concept.ids,
    type: 'CONCEPT_LIST',
    label: concept.label,
    tables,
  };
});

const transformStandardQueryToApi = (query, version) =>  {
  return {
    label: "Standard Query",
    type: 'CONCEPT_QUERY',
    root: {
      type: "AND",
      children: createQueryConcepts(query)
    }
  };
};

const createQueryDateRestriction = (dateRange, concept) => {
  return {
    type: "DATE_RESTRICTION",
    dateRange: dateRange,
    child: concept
  }
}

const createSavedQuery = (concept) => ({
  type: 'SAVED_QUERY',
  query: concept.id,
})

const createQueryConcept = (concept) =>
  concept.isPreviousQuery
    ? createSavedQuery(concept)
    : createConcept(concept)

const createConcept = (concept) => ({
  type: 'CONCEPT',
  ids: concept.ids,
  label: concept.label,
  tables: transformTablesToApi(concept.tables)
})

const createQueryConcepts = (query) => {
  return query.map(group => {
    const concepts = group.dateRange
      ? group.elements.map(concept =>
            createQueryDateRestriction(group.dateRange, createQueryConcept(concept)))
      : group.elements.map(concept => createQueryConcept(concept))

    return group.elements.length > 1
        ? { type: "OR", children: [...concepts]}
        : concepts.reduce((acc, curr) => ({ ...acc, ...curr }), {});
  })
}

const transformResultToApi = (result) => {
  return {
    id: result.id,
    timestamp: result.timestamp
  };
};

const getDayRange = (condition) => {
  if (condition.operator === DAYS_BEFORE)
    return [
      { minDays: condition.minDays },
      { maxDays: condition.maxDays },
    ];

  if (condition.operator === DAYS_OR_NO_EVENT_BEFORE)
    return [
      { minDays: condition.minDaysOrNoEvent },
      { maxDays: condition.maxDaysOrNoEvent },
    ];

  return [{}, {}];
};

const transformTimebasedQueryToApi = (query, version) => {
  return {
    version,
    type: 'TIME_QUERY',
    indexResult: query.indexResult,
    conditions: query.conditions.map(condition => {
      const [ minDays, maxDays ] = getDayRange(condition);

      return {
        operator: condition.operator,
        result0: transformResultToApi(condition.result0),
        result1: transformResultToApi(condition.result1),
        ...minDays,
        ...maxDays,
      };
    })
  };
};


// The query state already contains the query.
// But small additions are made (properties whitelisted), empty things filtered out
// to make it compatible with the backend API
export const transformQueryToApi = (query: Object, queryType: string, version: any) => {
  return queryType === 'timebased'
    ? transformTimebasedQueryToApi(query, version)
    : transformStandardQueryToApi(query, version);
};
