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
    version,
    type: 'CONCEPT_QUERY',
    groups: query.map(group => ({
      exclude: group.exclude,
      dateRange: group.dateRange ? group.dateRange : undefined,
      elements: group.elements.map(element => {
        if (element.isPreviousQuery) {
          return {
            id: element.id,
            type: 'QUERY',
            excludeTimestamps: element.excludeTimestamps,
          };
        } else {
          const tables = element.tables
            ? transformTablesToApi(element.tables)
            : [];

          return {
            ids: element.ids,
            type: 'CONCEPT_LIST',
            label: element.label,
            tables,
            excludeTimestamps: element.excludeTimestamps
          }
        }
      })
    }))
  };
};

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
