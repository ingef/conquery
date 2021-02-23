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

import { useApi, useApiUnauthorized } from "./useApi";
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
export const usePostQueries = () => {
  const api = useApi<PostQueriesResponseT>();

  return (
    datasetId: DatasetIdT,
    query: Object,
    options: { queryType: string; selectedSecondaryId?: string | null }
  ) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries`),
      method: "POST",
      data: transformQueryToApi(query, options), // Into backend-compatible format
    });
};

// Same signature as postQueries, plus a form query transformator
export const usePostFormQueries = () => {
  const api = useApi<PostQueriesResponseT>();

  return (
    datasetId: DatasetIdT,
    query: { form: any; formName: string },
    { formQueryTransformation }: { formQueryTransformation: Function }
  ) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries`),
      method: "POST",
      data: transformFormQueryToApi(query, formQueryTransformation), // Into backend-compatible format
    });
};

export const useDeleteQuery = () => {
  const api = useApi<null>();

  return (datasetId: DatasetIdT, queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
      method: "DELETE",
    });
};

export const useGetQuery = () => {
  const api = useApi<GetQueryResponseT>();

  return (datasetId: DatasetIdT, queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
    });
};

export const useGetForms = () => {
  const api = useApi<GetFormQueriesResponseT>();

  return (datasetId: DatasetIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-queries`),
    });
};

export const useGetStoredQueries = () => {
  const api = useApi<GetStoredQueriesResponseT>();

  return (datasetId: DatasetIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/stored-queries`),
    });
};

export const useGetStoredQuery = () => {
  const api = useApi<GetStoredQueryResponseT>();

  return (datasetId: DatasetIdT, queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/stored-queries/${queryId}`),
    });
};

export const useDeleteStoredQuery = () => {
  const api = useApi<null>();

  return (datasetId: DatasetIdT, queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/stored-queries/${queryId}`),
      method: "DELETE",
    });
};

export const usePatchStoredQuery = () => {
  const api = useApi<null>();

  return (datasetId: DatasetIdT, queryId: QueryIdT, attributes: Object) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/stored-queries/${queryId}`),
      method: "PATCH",
      data: attributes,
    });
};

export const usePostPrefixForSuggestions = () => {
  const api = useApi<PostFilterSuggestionsResponseT>();

  return (
    datasetId: DatasetIdT,
    conceptId: string,
    tableId: string,
    filterId: string,
    text: string
  ) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/concepts/${conceptId}/tables/${tableId}/filters/${filterId}/autocomplete`
      ),
      method: "POST",
      data: { text },
    });
};

export const usePostConceptsListToResolve = () => {
  const api = useApi<PostConceptResolveResponseT>();

  return (datasetId: DatasetIdT, conceptId: string, concepts: string[]) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/concepts/${conceptId}/resolve`
      ),
      method: "POST",
      data: { concepts },
    });
};

export const usePostFilterValuesResolve = () => {
  const api = useApi<PostFilterResolveResponseT>();

  return (
    datasetId: DatasetIdT,
    conceptId: string,
    tableId: string,
    filterId: string,
    values: string[]
  ) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/concepts/${conceptId}/tables/${tableId}/filters/${filterId}/resolve`
      ),
      method: "POST",
      data: { values },
    });
};

export const useGetMe = () => {
  return useApi<GetMeResponseT>({ url: getProtectedUrl(`/me`) });
};

export const usePostLogin = () => {
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
};

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
