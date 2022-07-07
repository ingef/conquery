import { useCallback } from "react";

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
  GetEntityHistoryResponse,
  GetEntityHistoryDefaultParamsResponse,
  TableT,
  DatasetT,
} from "./types";
import { useApi, useApiUnauthorized } from "./useApi";

const PROTECTED_PREFIX = "/api";

function getProtectedUrl(url: string) {
  return apiUrl + PROTECTED_PREFIX + url;
}

export const useGetFrontendConfig = () => {
  const api = useApi<GetFrontendConfigResponseT>();

  return useCallback(
    () =>
      api({
        url: getProtectedUrl("/config/frontend"),
      }),
    [api],
  );
};

export const useGetDatasets = () => {
  const api = useApi<GetDatasetsResponseT>();

  return useCallback(() => api({ url: getProtectedUrl(`/datasets`) }), [api]);
};

export const useGetConcepts = () => {
  const api = useApi<GetConceptsResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"]) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/concepts`),
      }),
    [api],
  );
};

export const useGetConcept = () => {
  const api = useApi<GetConceptResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], conceptId: ConceptIdT) =>
      api(
        {
          url: getProtectedUrl(`/datasets/${datasetId}/concepts/${conceptId}`),
        },
        {
          etagCacheKey: `${datasetId}-${conceptId}`,
        },
      ),
    [api],
  );
};

// Same signature as postFormQueries
export const usePostQueries = () => {
  const api = useApi<PostQueriesResponseT>();

  return useCallback(
    (
      datasetId: DatasetT["id"],
      query: StandardQueryStateT | ValidatedTimebasedQueryStateT,
      options: { queryType: string; selectedSecondaryId?: string | null },
    ) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries`),
        method: "POST",
        data: transformQueryToApi(query, options), // Into backend-compatible format
      }),
    [api],
  );
};

export interface FormQueryPostPayload {
  type: string;
  values: any;
  [fieldName: string]: unknown;
}
// Same signature as postQueries, plus a form query transformator
export const usePostFormQueries = () => {
  const api = useApi<PostQueriesResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], query: FormQueryPostPayload) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries`),
        method: "POST",
        data: query,
      }),
    [api],
  );
};

export const useGetQuery = () => {
  const api = useApi<GetQueryResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], queryId: QueryIdT) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
      }),
    [api],
  );
};

export const usePostQueryCancel = () => {
  const api = useApi<null>();

  return useCallback(
    (datasetId: DatasetT["id"], queryId: QueryIdT) =>
      api({
        url: getProtectedUrl(
          `/datasets/${datasetId}/queries/${queryId}/cancel`,
        ),
        method: "POST",
      }),
    [api],
  );
};

export const usePostQueryUpload = () => {
  const api = useApi<UploadQueryResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], data: QueryToUploadT) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries/upload`),
        method: "POST",
        data,
      }),
    [api],
  );
};

export const useDeleteQuery = () => {
  const api = useApi<null>();

  return useCallback(
    (datasetId: DatasetT["id"], queryId: QueryIdT) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
        method: "DELETE",
      }),
    [api],
  );
};

export const useGetForms = () => {
  const api = useApi<GetFormQueriesResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"]) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/form-queries`),
      }),
    [api],
  );
};

export const useGetQueries = () => {
  const api = useApi<GetQueriesResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"]) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries`),
      }),
    [api],
  );
};

export const usePatchQuery = () => {
  const api = useApi<null>();

  return useCallback(
    (datasetId: DatasetT["id"], queryId: QueryIdT, attributes: Object) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries/${queryId}`),
        method: "PATCH",
        data: attributes,
      }),
    [api],
  );
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

  return useCallback(
    ({
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
      }),
    [api],
  );
};

export const usePostConceptsListToResolve = () => {
  const api = useApi<PostConceptResolveResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], conceptId: string, concepts: string[]) =>
      api({
        url: getProtectedUrl(
          `/datasets/${datasetId}/concepts/${conceptId}/resolve`,
        ),
        method: "POST",
        data: { concepts },
      }),
    [api],
  );
};

export const usePostFilterValuesResolve = () => {
  const api = useApi<PostFilterResolveResponseT>();

  return useCallback(
    (
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
      }),
    [api],
  );
};

export const useGetMe = () => {
  const api = useApi<GetMeResponseT>();
  return useCallback(() => api({ url: getProtectedUrl(`/me`) }), [api]);
};

export const usePostLogin = () => {
  const api = useApiUnauthorized<PostLoginResponseT>({
    url: apiUrl + "/auth",
    method: "POST",
  });

  return useCallback(
    (user: string, password: string) =>
      api({
        data: {
          user,
          password,
        },
      }),
    [api],
  );
};

// This endpoint exists, but it's not used anymore,
// since form configs are auto-saved when starting a form query.
// TODO: remove if not needed after a while
export const usePostFormConfig = () => {
  const api = useApi<PostFormConfigsResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], data: BaseFormConfigT) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/form-configs`),
        method: "POST",
        data,
      }),
    [api],
  );
};

export const useGetFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"], formConfigId: string) =>
      api({
        url: getProtectedUrl(
          `/datasets/${datasetId}/form-configs/${formConfigId}`,
        ),
      }),
    [api],
  );
};

export const usePatchFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return useCallback(
    (
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
      }),
    [api],
  );
};

export const useGetFormConfigs = () => {
  const api = useApi<GetFormConfigsResponseT>();

  return useCallback(
    (datasetId: DatasetT["id"]) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/form-configs`),
      }),
    [api],
  );
};

export const useDeleteFormConfig = () => {
  const api = useApi<null>();

  return useCallback(
    (datasetId: DatasetT["id"], formConfigId: string) =>
      api({
        url: getProtectedUrl(
          `/datasets/${datasetId}/form-configs/${formConfigId}`,
        ),
        method: "DELETE",
      }),
    [api],
  );
};

export const useGetEntityHistoryDefaultParams = () => {
  const api = useApi<GetEntityHistoryDefaultParamsResponse>();

  return useCallback(
    (datasetId: DatasetT["id"]) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/entity-preview`),
      }),
    [api],
  );
};

export const useGetEntityHistory = () => {
  const api = useApi<GetEntityHistoryResponse>();

  return useCallback(
    (
      datasetId: DatasetT["id"],
      entityId: string,
      sources: TableT["id"][],
      time: {
        min: string; // Format like "2020-01-01"
        max: string; // Format like "2020-12-31"
      } = {
        min: "2015-01-01",
        max: "2021-12-31",
      },
    ) =>
      api({
        method: "POST",
        url: getProtectedUrl(`/datasets/${datasetId}/queries/entity`),
        data: {
          idKind: "PID", // TODO: Figure out which other strings are possible here
          entityId,
          time,
          sources,
        },
      }),
    [api],
  );
};
