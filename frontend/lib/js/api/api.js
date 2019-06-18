// @flow

import { apiUrl } from "../environment";

import { type DatasetIdType } from "../dataset/reducer";
import type {
  ConceptIdT,
  GetFrontendConfigResponseT,
  GetConceptsResponseT,
  GetConceptResponseT,
  PostQueriesResponseT,
  GetQueryResponseT,
  GetStoredQueriesResponseT,
  GetStoredQueryResponseT,
  PostConceptResolveResponseT,
  PostFilterResolveResponseT,
  PostFilterSuggestionsResponseT
} from "./types";

import fetchJson from "./fetchJson";
import { transformQueryToApi } from "./apiHelper";
import { transformFormQueryToApi } from "./apiExternalFormsHelper";

export function getFrontendConfig(): Promise<GetFrontendConfigResponseT> {
  return fetchJson(apiUrl() + "/config/frontend");
}

export function getDatasets() {
  return fetchJson(apiUrl() + `/datasets`);
}

export const getConcepts = (
  datasetId: DatasetIdType
): Promise<GetConceptsResponseT> => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts`);
};

export const getConcept = (
  datasetId: DatasetIdType,
  conceptId: ConceptIdT
): Promise<GetConceptResponseT> => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts/${conceptId}`);
};

// Same signature as postFormQueries
export function postQueries(
  datasetId: DatasetIdType,
  query: Object,
  queryType: string
): Promise<PostQueriesResponseT> {
  // Transform into backend-compatible format
  const body = transformQueryToApi(query, queryType);

  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries`, {
    method: "POST",
    body
  });
}

export function deleteQuery(
  datasetId: DatasetIdType,
  queryId: number
): Promise<null> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries/${queryId}`, {
    method: "DELETE"
  });
}

export function getQuery(
  datasetId: DatasetIdType,
  queryId: number
): Promise<GetQueryResponseT> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries/${queryId}`);
}

// Same signature as postQueries, plus a form query transformator
export function postFormQueries(
  datasetId: DatasetIdType,
  query: Object,
  queryType: string,
  version: any,
  formQueryTransformation: Function
): Promise<PostQueriesResponseT> {
  // Transform into backend-compatible format
  const body = transformFormQueryToApi(query, version, formQueryTransformation);

  return fetchJson(apiUrl() + `/datasets/${datasetId}/form-queries`, {
    method: "POST",
    body
  });
}

export function deleteFormQuery(
  datasetId: DatasetIdType,
  queryId: number
): Promise<null> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/form-queries/${queryId}`,
    {
      method: "DELETE"
    }
  );
}

export function getFormQuery(
  datasetId: DatasetIdType,
  queryId: number
): Promise<GetQueryResponseT> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/form-queries/${queryId}`);
}

export function getStoredQueries(
  datasetId: DatasetIdType
): Promise<GetStoredQueriesResponseT> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/stored-queries`);
}

export function getStoredQuery(
  datasetId: DatasetIdType,
  queryId: number
): Promise<GetStoredQueryResponseT> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`
  );
}

export function deleteStoredQuery(
  datasetId: DatasetIdType,
  queryId: number
): Promise<null> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`,
    {
      method: "DELETE"
    }
  );
}

export function patchStoredQuery(
  datasetId: DatasetIdType,
  queryId: number,
  attributes: Object
): Promise<null> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`,
    {
      method: "PATCH",
      body: attributes
    }
  );
}

export function postPrefixForSuggestions(
  datasetId: DatasetIdType,
  conceptId: string,
  tableId: string,
  filterId: string,
  text: string
): Promise<PostFilterSuggestionsResponseT> {
  return fetchJson(
    apiUrl() +
      `/datasets/${datasetId}/concepts/${conceptId}` +
      `/tables/${tableId}/filters/${filterId}/autocomplete`,
    {
      method: "POST",
      body: { text }
    }
  );
}

export function postConceptsListToResolve(
  datasetId: DatasetIdType,
  conceptId: string,
  concepts: string[]
): Promise<PostConceptResolveResponseT> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/concepts/${conceptId}/resolve`,
    {
      method: "POST",
      body: { concepts }
    }
  );
}

export function postFilterValuesResolve(
  datasetId: DatasetIdType,
  conceptId: string,
  tableId: string,
  filterId: string,
  values: string[]
): Promise<PostFilterResolveResponseT> {
  return fetchJson(
    apiUrl() +
      `/datasets/${datasetId}/concepts/${conceptId}` +
      `/tables/${tableId}/filters/${filterId}/resolve`,
    {
      method: "POST",
      body: { values }
    }
  );
}
