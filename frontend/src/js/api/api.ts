import { useCallback } from "react";

import { EditorV2Query } from "../editor-v2/types";
import { EntityId } from "../entity-history/reducer";
import { apiUrl } from "../environment";
import type { FormConfigT } from "../previous-queries/list/reducer";
import type { QueryToUploadT } from "../previous-queries/upload/CSVColumnPicker";
import { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import { ValidatedTimebasedQueryStateT } from "../timebased-query-editor/reducer";

import { transformQueryToApi } from "./apiHelper";
import {
  type QueryIdT,
  type ConceptIdT,
  type GetFrontendConfigResponseT,
  type GetConceptsResponseT,
  type GetConceptResponseT,
  type PostQueriesResponseT,
  type GetQueryResponseT,
  type GetQueriesResponseT,
  type PostConceptResolveResponseT,
  type PostFilterResolveResponseT,
  type PostFilterSuggestionsResponseT,
  type GetFormQueriesResponseT,
  type GetMeResponseT,
  type PostLoginResponseT,
  type GetFormConfigsResponseT,
  type GetFormConfigResponseT,
  type GetDatasetsResponseT,
  type UploadQueryResponseT,
  type GetEntityHistoryResponse,
  type GetEntityHistoryDefaultParamsResponse,
  type DatasetT,
  type HistorySources,
  type PostResolveEntitiesResponse,
  PreviewStatisticsResponse,
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
          url: getProtectedUrl(`/concepts/${conceptId}`),
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
      query:
        | StandardQueryStateT
        | ValidatedTimebasedQueryStateT
        | EditorV2Query,
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
    (queryId: QueryIdT) =>
      api({
        url: getProtectedUrl(`/queries/${queryId}`),
      }),
    [api],
  );
};

export const usePostQueryCancel = () => {
  const api = useApi<null>();

  return useCallback(
    (queryId: QueryIdT) =>
      api({
        url: getProtectedUrl(`/queries/${queryId}/cancel`),
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
    (queryId: QueryIdT) =>
      api({
        url: getProtectedUrl(`/queries/${queryId}`),
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
    (queryId: QueryIdT, attributes: Object) =>
      api({
        url: getProtectedUrl(`/queries/${queryId}`),
        method: "PATCH",
        data: attributes,
      }),
    [api],
  );
};

export interface PostPrefixForSuggestionsParams {
  filterId: string;
  prefix: string;
  page: number;
  pageSize: number;
}
export const usePostPrefixForSuggestions = () => {
  const api = useApi<PostFilterSuggestionsResponseT>();

  return useCallback(
    ({ filterId, prefix, page, pageSize }: PostPrefixForSuggestionsParams) =>
      api({
        url: getProtectedUrl(`/filters/${filterId}/autocomplete`),
        method: "POST",
        data: { text: prefix, page, pageSize },
      }),
    [api],
  );
};

export const usePostConceptsListToResolve = () => {
  const api = useApi<PostConceptResolveResponseT>();

  return useCallback(
    (conceptId: string, concepts: string[]) =>
      api({
        url: getProtectedUrl(`/concepts/${conceptId}/resolve`),
        method: "POST",
        data: { concepts },
      }),
    [api],
  );
};

export const usePostFilterValuesResolve = () => {
  const api = useApi<PostFilterResolveResponseT>();

  return useCallback(
    (filterId: string, values: string[]) =>
      api({
        url: getProtectedUrl(`/filters/${filterId}/resolve`),
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

export const useGetFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return useCallback(
    (formConfigId: string) =>
      api({
        url: getProtectedUrl(`/form-configs/${formConfigId}`),
      }),
    [api],
  );
};

export const usePatchFormConfig = () => {
  const api = useApi<GetFormConfigResponseT>();

  return useCallback(
    (formConfigId: string, data: Partial<FormConfigT>) =>
      api({
        url: getProtectedUrl(`/form-configs/${formConfigId}`),
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
    (formConfigId: string) =>
      api({
        url: getProtectedUrl(`/form-configs/${formConfigId}`),
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
      entityId: EntityId,
      sources: HistorySources,
      time: {
        min: string; // Format like "2020-01-01"
        max: string; // Format like "2020-12-31"
      },
    ) =>
      api({
        method: "POST",
        url: getProtectedUrl(`/datasets/${datasetId}/queries/entity`),
        data: {
          idKind: entityId.kind,
          entityId: entityId.id,
          time,
          sources: sources.all.map((s) => s.name),
        },
      }),
    [api],
  );
};

export const usePostResolveEntities = () => {
  const api = useApi<PostResolveEntitiesResponse>();

  return useCallback(
    (
      datasetId: DatasetT["id"],
      filterValues: {
        filter: string; // id
        type: "MULTI_SELECT" | "BIG_MULTI_SELECT";
        value: string[];
      }[],
    ) =>
      api({
        url: getProtectedUrl(`/datasets/${datasetId}/queries/resolve-entities`),
        method: "POST",
        data: filterValues,
      }),
    [api],
  );
};

export const useGetResult = () => {
  return useCallback(
    (queryId: string) => fetch(getProtectedUrl(`/result/arrow/${queryId}.arrs`)),
    [],
  );
};

export const usePreviewStatistics = () => {
  const api = useApi<PreviewStatisticsResponse>();

  return useCallback(
    (
      queryId: string
    ) =>
      api({
        url: getProtectedUrl(`/queries/${queryId}/statistics`),
        method: "GET",
        data: queryId,
      }),
    [api],
  );

};
