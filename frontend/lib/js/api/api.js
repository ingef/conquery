// @flow

import { apiUrl } from "../environment";

import type { DatasetIdT, QueryIdT } from "../api/types";
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
  PostFilterSuggestionsResponseT,
  GetMeResponseT
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
  datasetId: DatasetIdT
): Promise<GetConceptsResponseT> => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts`);
};

export const getConcept = (
  datasetId: DatasetIdT,
  conceptId: ConceptIdT
): Promise<GetConceptResponseT> => {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/concepts/${conceptId}`);
};

// Same signature as postFormQueries
export function postQueries(
  datasetId: DatasetIdT,
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
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<null> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries/${queryId}`, {
    method: "DELETE"
  });
}

export function getQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<GetQueryResponseT> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/queries/${queryId}`);
}

// Same signature as postQueries, plus a form query transformator
export function postFormQueries(
  datasetId: DatasetIdT,
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
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<null> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/form-queries/${queryId}`,
    {
      method: "DELETE"
    }
  );
}

export function getFormQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<GetQueryResponseT> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/form-queries/${queryId}`);
}

export function getStoredQueries(
  datasetId: DatasetIdT
): Promise<GetStoredQueriesResponseT> {
  return fetchJson(apiUrl() + `/datasets/${datasetId}/stored-queries`);
}

export function getStoredQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<GetStoredQueryResponseT> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`
  );
}

export function deleteStoredQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<null> {
  return fetchJson(
    apiUrl() + `/datasets/${datasetId}/stored-queries/${queryId}`,
    {
      method: "DELETE"
    }
  );
}

export function patchStoredQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT,
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
  datasetId: DatasetIdT,
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
  datasetId: DatasetIdT,
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
  datasetId: DatasetIdT,
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

export function getMe(): Promise<GetMeResponseT> {
  return fetchJson(apiUrl() + `/me`);
}
