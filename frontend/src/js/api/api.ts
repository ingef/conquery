import { apiUrl } from "../environment";
import type {
  BaseFormConfigT,
  FormConfigT,
} from "../previous-queries/list/reducer";
import type { QueryToUploadT } from "../previous-queries/upload/CSVColumnPicker";
import { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import { ValidatedTimebasedQueryStateT } from "../timebased-query-editor/reducer";

import { transformQueryToApi } from "./apiHelper";
import type {
  QueryIdT,
  ConceptIdT,
  GetFrontendConfigResponseT,
  GetConceptsResponseT,
  GetConceptResponseT,
  PostQueriesResponseT,
  GetQueryResponseT,
  GetQueriesResponseT,
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
  UploadQueryResponseT,
  DatasetT,
} from "./types";
import { useApi, useApiUnauthorized } from "./useApi";

const PROTECTED_PREFIX = "/api";

function getProtectedUrl(url: string) {
  return apiUrl + PROTECTED_PREFIX + url;
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

  return (datasetId: DatasetT["id"]) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/concepts`),
    });
};

export const useGetConcept = () => {
  const api = useApi<GetConceptResponseT>({});

  return (datasetId: DatasetT["id"], conceptId: ConceptIdT) =>
    api(
      {
        url: getProtectedUrl(`/datasets/${datasetId}/concepts/${conceptId}`),
      },
      {
        etagCacheKey: `${datasetId}-${conceptId}`,
      },
    );
};

// Same signature as postFormQueries
export const usePostQueries = () => {
  const api = useApi<PostQueriesResponseT>();

  return (
    datasetId: DatasetT["id"],
    query: StandardQueryStateT | ValidatedTimebasedQueryStateT,
    options: { queryType: string; selectedSecondaryId?: string | null },
  ) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries`),
      method: "POST",
      data: transformQueryToApi(query, options), // Into backend-compatible format
    });
};

export interface FormQueryPostPayload {
  type: string;
  values: any;
  [fieldName: string]: unknown;
}
// Same signature as postQueries, plus a form query transformator
export const usePostFormQueries = () => {
  const api = useApi<PostQueriesResponseT>();

  return (datasetId: DatasetT["id"], query: FormQueryPostPayload) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries`),
      method: "POST",
      data: query,
    });
};

export const useGetQuery = () => {
  const api = useApi<GetQueryResponseT>();

  return (datasetId: DatasetT["id"], queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
    });
};

export const usePostQueryCancel = () => {
  const api = useApi<null>();

  return (datasetId: DatasetT["id"], queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}/cancel`),
      method: "POST",
    });
};

export const usePostQueryUpload = () => {
  const api = useApi<UploadQueryResponseT>();

  return (datasetId: DatasetT["id"], data: QueryToUploadT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/upload`),
      method: "POST",
      data,
    });
};

export const useDeleteQuery = () => {
  const api = useApi<null>();

  return (datasetId: DatasetT["id"], queryId: QueryIdT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
      method: "DELETE",
    });
};

export const useGetForms = () => {
  const api = useApi<GetFormQueriesResponseT>();

  return (datasetId: DatasetT["id"]) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-queries`),
    });
};

export const useGetQueries = () => {
  const api = useApi<GetQueriesResponseT>();

  return (datasetId: DatasetT["id"]) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries`),
    });
};

export const usePatchQuery = () => {
  const api = useApi<null>();

  return (datasetId: DatasetT["id"], queryId: QueryIdT, attributes: Object) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
      method: "PATCH",
      data: attributes,
    });
};

export interface PostPrefixForSuggestionsParams {
  datasetId: DatasetT["id"];
  conceptId: string;
  tableId: string;
  filterId: string;
  prefix: string;
  page: number;
  pageSize: number;
}
export const usePostPrefixForSuggestions = () => {
  const api = useApi<PostFilterSuggestionsResponseT>();

  return ({
    datasetId,
    conceptId,
    tableId,
    filterId,
    prefix,
    page,
    pageSize,
  }: PostPrefixForSuggestionsParams) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/concepts/${conceptId}/tables/${tableId}/filters/${filterId}/autocomplete`,
      ),
      method: "POST",
      data: { text: prefix, page, pageSize },
    });
};

export const usePostConceptsListToResolve = () => {
  const api = useApi<PostConceptResolveResponseT>();

  return (datasetId: DatasetT["id"], conceptId: string, concepts: string[]) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/concepts/${conceptId}/resolve`,
      ),
      method: "POST",
      data: { concepts },
    });
};

export const usePostFilterValuesResolve = () => {
  const api = useApi<PostFilterResolveResponseT>();

  return (
    datasetId: DatasetT["id"],
    conceptId: string,
    tableId: string,
    filterId: string,
    values: string[],
  ) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/concepts/${conceptId}/tables/${tableId}/filters/${filterId}/resolve`,
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
    url: apiUrl + "/auth",
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

// This endpoint exists, but it's not used anymore,
// since form configs are auto-saved when starting a form query.
// TODO: remove if not needed after a while
export const usePostFormConfig = () => {
  const api = useApi<PostFormConfigsResponseT>();

  return (datasetId: DatasetT["id"], data: BaseFormConfigT) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-configs`),
      method: "POST",
      data,
    });
};

export const useGetFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return (datasetId: DatasetT["id"], formConfigId: string) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/form-configs/${formConfigId}`,
      ),
    });
};

export const usePatchFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return (
    datasetId: DatasetT["id"],
    formConfigId: string,
    data: Partial<FormConfigT>,
  ) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/form-configs/${formConfigId}`,
      ),
      method: "PATCH",
      data,
    });
};

export const useGetFormConfigs = () => {
  const api = useApi<GetFormConfigsResponseT>();

  return (datasetId: DatasetT["id"]) =>
    api({
      url: getProtectedUrl(`/datasets/${datasetId}/form-configs`),
    });
};

export const useDeleteFormConfig = () => {
  const api = useApi<null>();

  return (datasetId: DatasetT["id"], formConfigId: string) =>
    api({
      url: getProtectedUrl(
        `/datasets/${datasetId}/form-configs/${formConfigId}`,
      ),
      method: "DELETE",
    });
};
