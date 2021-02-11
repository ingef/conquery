import { apiUrl } from "../environment";

import type {
  DatasetIdT,
  QueryIdT,
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
  GetFormQueriesResponseT,
  GetMeResponseT,
  PostLoginResponseT,
  PostFormConfigsResponseT,
  GetFormConfigsResponseT,
  GetFormConfigResponseT,
  GetDatasetsResponseT,
} from "./types";

import fetchJson, { useApi, useApiUnauthorized } from "./useApi";
import { transformQueryToApi } from "./apiHelper";
import { transformFormQueryToApi } from "./apiExternalFormsHelper";
import type {
  FormConfigT,
  BaseFormConfigT,
} from "../external-forms/form-configs/reducer";

const PROTECTED_PREFIX = "/api";

function getProtectedUrl(url: string) {
  return apiUrl() + PROTECTED_PREFIX + url;
}

export const useGetFrontendConfig = () => {
  return useApi<GetFrontendConfigResponseT>({
    url: getProtectedUrl("/config/frontend"),
  });
};

export const useGetDatasets = () => {
  return useApi<GetDatasetsResponseT>({ url: getProtectedUrl(`/datasets`) });
};

export const useGetConcepts = () => {
  const api = useApi<GetConceptsResponseT>({});

  return (datasetId: DatasetIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/concepts`),
    });
};

export const useGetConcept = () => {
  const api = useApi<GetConceptResponseT>({});

  return (datasetId: DatasetIdT, conceptId: ConceptIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/concepts/${conceptId}`),
    });
};

// Same signature as postFormQueries
export function postQueries(
  datasetId: DatasetIdT,
  query: Object,
  options: { queryType: string; selectedSecondaryId?: string | null }
): Promise<PostQueriesResponseT> {
  // Transform into backend-compatible format
  const data = transformQueryToApi(query, options);

  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/queries`),
    method: "POST",
    data,
  });
}

// Same signature as postQueries, plus a form query transformator
export function postFormQueries(
  datasetId: DatasetIdT,
  query: { form: string; formName: string },
  { formQueryTransformation }: { formQueryTransformation: Function }
): Promise<PostQueriesResponseT> {
  // Transform into backend-compatible format
  const data = transformFormQueryToApi(query, formQueryTransformation);

  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/queries`),
    method: "POST",
    data,
  });
}

export function deleteQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<null> {
  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
    method: "DELETE",
  });
}

export function getQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<GetQueryResponseT> {
  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
  });
}

export const useGetForms = () => {
  const api = useApi<GetFormQueriesResponseT>();

  return (datasetId: DatasetIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-queries`),
    });
};

export function getStoredQueries(
  datasetId: DatasetIdT
): Promise<GetStoredQueriesResponseT> {
  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/stored-queries`),
  });
}

export function getStoredQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<GetStoredQueryResponseT> {
  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/stored-queries/${queryId}`),
  });
}

export function deleteStoredQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT
): Promise<null> {
  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/stored-queries/${queryId}`),
    method: "DELETE",
  });
}

export function patchStoredQuery(
  datasetId: DatasetIdT,
  queryId: QueryIdT,
  attributes: Object
): Promise<null> {
  return fetchJson({
    url: getProtectedUrl(`/datasets/${datasetId}/stored-queries/${queryId}`),
    method: "PATCH",
    data: attributes,
  });
}

export function postPrefixForSuggestions(
  datasetId: DatasetIdT,
  conceptId: string,
  tableId: string,
  filterId: string,
  text: string
): Promise<PostFilterSuggestionsResponseT> {
  return fetchJson({
    url: getProtectedUrl(
      `/datasets/${datasetId}/concepts/${conceptId}/tables/${tableId}/filters/${filterId}/autocomplete`
    ),
    method: "POST",
    data: { text },
  });
}

export function postConceptsListToResolve(
  datasetId: DatasetIdT,
  conceptId: string,
  concepts: string[]
): Promise<PostConceptResolveResponseT> {
  return fetchJson({
    url: getProtectedUrl(
      `/datasets/${datasetId}/concepts/${conceptId}/resolve`
    ),
    method: "POST",
    data: { concepts },
  });
}

export function postFilterValuesResolve(
  datasetId: DatasetIdT,
  conceptId: string,
  tableId: string,
  filterId: string,
  values: string[]
): Promise<PostFilterResolveResponseT> {
  return fetchJson({
    url: getProtectedUrl(
      `/datasets/${datasetId}/concepts/${conceptId}/tables/${tableId}/filters/${filterId}/resolve`
    ),
    method: "POST",
    data: { values },
  });
}

export const useGetMe = () => {
  return useApi<GetMeResponseT>({ url: getProtectedUrl(`/me`) });
};

export function usePostLogin() {
  const api = useApiUnauthorized<PostLoginResponseT>({
    url: apiUrl() + "/auth",
    method: "POST",
  });

  return (user: string, password: string) =>
    api({
      data: {
        user,
        password,
      },
    });
}

export const usePostFormConfig = () => {
  const api = useApi<PostFormConfigsResponseT>();

  return (datasetId: DatasetIdT, data: BaseFormConfigT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-configs`),
      method: "POST",
      data,
    });
};

export const useGetFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return (datasetId: DatasetIdT, formConfigId: string) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/form-configs/${formConfigId}`
      ),
    });
};

export const usePatchFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return (
    datasetId: DatasetIdT,
    formConfigId: string,
    data: Partial<FormConfigT>
  ) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/form-configs/${formConfigId}`
      ),
      method: "PATCH",
      data,
    });
};

export const useGetFormConfigs = () => {
  const api = useApi<GetFormConfigsResponseT>();

  return (datasetId: DatasetIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-configs`),
    });
};

export const useDeleteFormConfig = () => {
  const api = useApi<null>();

  return (datasetId: DatasetIdT, formConfigId: string) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/form-configs/${formConfigId}`
      ),
      method: "DELETE",
    });
};
