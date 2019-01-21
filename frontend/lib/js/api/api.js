// @flow

import fetch                   from 'isomorphic-fetch';

import { getStoredAuthToken }  from '../authorization';
import { apiUrl }              from '../environment';

import { type DatasetIdType }  from '../dataset/reducer';
import type {
  RootType,
  TreeNodeIdType,
  ConceptListResolutionResultType
}                              from '../common/types/backend';

import {
  transformQueryToApi,
} from './apiHelper';

import {
  transformFormQueryToApi,
} from './apiExternalFormsHelper';

type RequestType = {
  body?: Object | string,
  headers?: Object
};

function fetchJsonUnauthorized(url: string, request?: RequestType, rawBody?: boolean = false) {
  const finalRequest = request
    ? {
        ...request,
        body: rawBody ? request.body : JSON.stringify(request.body),
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json",
          ...request.headers,
        },
      }
    : {
        method: "GET",
        headers: {
          "Accept": "application/json",
        },
      };

  return fetch(url, finalRequest).then(
    response => {
      if (response.status >= 200 && response.status < 300)
        return response.json().catch(e => e); // Also handle empty responses
      else
        // Reject other status
        return response.json().then(Promise.reject.bind(Promise));
    },
    error => Promise.reject(error) // Network or connection failure
  );
}

function fetchJson(url: string, request?: RequestType, rawBody?: boolean = false) {
  const authToken = getStoredAuthToken() || '';
  const finalRequest = {
    ...(request || {}),
    headers: {
      "Authorization": `Bearer ${authToken}`,
      ...((request && request.headers) || {}),
    }
  };

  return fetchJsonUnauthorized(url, finalRequest, rawBody);
}

export function getFrontendConfig() {
  return fetchJson(apiUrl() + '/config/frontend')
}

export function getDatasets() {
  return fetchJson(apiUrl() + `/datasets`);
}

export const getConcepts = (datasetId: DatasetIdType) : Promise<RootType> => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts`);
}

export type ConceptElementType = {
  children: Array<TreeNodeIdType>,
};

export const getConcept =
  (datasetId: DatasetIdType, conceptId: TreeNodeIdType)
    : Promise<Map<TreeNodeIdType, ConceptElementType>> => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts/${conceptId}`);
}

// Same signature as postFormQueries
export function postQueries(
  datasetId: DatasetIdType,
  query: Object,
  queryType: string,
  version: any
) {
  // Transform into backend-compatible format
  const body = transformQueryToApi(query, queryType, version);

  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries`, {
    method: "POST",
    body,
  }, queryType === 'external');
}

export function deleteQuery(datasetId: DatasetIdType, queryId: number) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries/${queryId}`, {
    method: 'DELETE',
  });
}

export function getQuery(datasetId: DatasetIdType, queryId: number) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries/${queryId}`);
}

// Same signature as postQueries, plus a form query transformator
export function postFormQueries(
  datasetId: DatasetIdType,
  query: Object,
  queryType: string,
  version: any,
  formQueryTransformation: Function
) {
  // Transform into backend-compatible format
  const body = transformFormQueryToApi(query, version, formQueryTransformation);

  return fetchJson(apiUrl() + `/datasets/${datasetId}/form-queries`, {
    method: "POST",
    body,
  });
}

export function deleteFormQuery(datasetId: DatasetIdType, queryId: number) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/form-queries/${queryId}`, {
    method: 'DELETE',
  });
}

export function getFormQuery(datasetId: DatasetIdType, queryId: number) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/form-queries/${queryId}`);
}

export function getStoredQueries(datasetId: DatasetIdType) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/stored-queries`);
}

export function getStoredQuery(datasetId: DatasetIdType, queryId: number) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`);
}

export function deleteStoredQuery(datasetId: DatasetIdType, queryId: number) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`, {
    method: 'DELETE',
  });
}

export function patchStoredQuery(datasetId: DatasetIdType, queryId: number, attributes: Object) {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`, {
    method: 'PATCH',
    body: attributes,
  });
}

export function postPrefixForSuggestions(
  datasetId: DatasetIdType,
  conceptId: string,
  tableId: string,
  filterId: string,
  text: string,
) {
  return fetchJson(
    apiUrl() +
    `/datasets/${datasetId}/concepts/${conceptId}` +
    `/tables/${tableId}/filters/${filterId}/autocomplete`,
    {
      method: 'POST',
      body: { text },
    }
  );
};

export function postConceptsListToResolve(
  datasetId: DatasetIdType,
  conceptId: string,
  concepts: string[],
): ConceptListResolutionResultType {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts/${conceptId}/resolve`, {
    method: 'POST',
    body: { concepts },
  });
};

export function postConceptFilterValuesResolve(
  datasetId: DatasetIdType,
  conceptId: string,
  tableId: string,
  filterId: string,
  values: string[],
) {
  return fetchJson(
    apiUrl() +
    `/datasets/${datasetId}/concepts/${conceptId}` +
    `/tables/${tableId}/filters/${filterId}/resolve`,
    {
    method: 'POST',
      body: { values },
    }
  );
};

export const searchConcepts = (datasetId: DatasetIdType, query: string, limit?: number) => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts/search`, {
    method: 'POST',
    body: {
      query: query,
      limit: limit || 50
    }
  });
}
