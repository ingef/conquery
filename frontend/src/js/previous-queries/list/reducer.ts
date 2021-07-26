import { getType } from "typesafe-actions";

import type { QueryIdT, UserGroupIdT } from "../../api/types";
import { Action } from "../../app/actions";

import {
  deleteQuerySuccess,
  loadQueries,
  loadQuery,
  renameQuery,
  retagQuery,
  shareQuerySuccess,
} from "./actions";

export type PreviousQueryIdT = string;
export interface PreviousQueryT {
  id: PreviousQueryIdT;
  label: string;
  loading?: boolean;
  error?: string | null;
  numberOfResults: number | null;
  createdAt: string;
  tags: string[];
  own: boolean;
  ownerName: string;
  system?: boolean;
  resultUrls: string[];
  shared: boolean;
  isPristineLabel?: boolean;
  groups?: UserGroupIdT[];
  queryType: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
  secondaryId?: string | null;
}

export interface PreviousQueriesStateT {
  queries: PreviousQueryT[];
  loading: boolean;
  error: string | null;
}

const initialState: PreviousQueriesStateT = {
  queries: [],
  loading: false,
  error: null,
};

const findQuery = (queries: PreviousQueryT[], queryId: string | number) => {
  const query = queries.find((q) => q.id === queryId);

  return {
    query,
    queryIdx: query ? queries.indexOf(query) : -1,
  };
};

const updatePreviousQuery = (
  state: PreviousQueriesStateT,
  { payload: { queryId } }: { payload: { queryId: QueryIdT } },
  attributes: Partial<PreviousQueryT>,
) => {
  const { query, queryIdx } = findQuery(state.queries, queryId);

  if (!query) return state;

  return {
    ...state,
    queries: [
      ...state.queries.slice(0, queryIdx),
      {
        ...query,
        ...attributes,
      },
      ...state.queries.slice(queryIdx + 1),
    ],
  };
};

const sortQueries = (queries: PreviousQueryT[]) => {
  return queries.sort((a, b) => {
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });
};

const deletePreviousQuery = (
  state: PreviousQueriesStateT,
  { queryId }: { queryId: QueryIdT },
) => {
  const { queryIdx } = findQuery(state.queries, queryId);

  return {
    ...state,
    queries: [
      ...state.queries.slice(0, queryIdx),
      ...state.queries.slice(queryIdx + 1),
    ],
  };
};

const previousQueriesReducer = (
  state: PreviousQueriesStateT = initialState,
  action: Action,
): PreviousQueriesStateT => {
  switch (action.type) {
    case getType(loadQueries.request):
      return { ...state, loading: true };
    case getType(loadQueries.success):
      return {
        ...state,
        loading: false,
        queries: sortQueries(action.payload.data),
      };
    case getType(loadQueries.failure):
      return {
        ...state,
        loading: false,
        error: action.payload.message || null,
      };
    case getType(loadQuery.request):
    case getType(renameQuery.request):
    case getType(retagQuery.request):
      return updatePreviousQuery(state, action, { loading: true });
    case getType(loadQuery.success):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        ...action.payload.data,
      });
    case getType(renameQuery.success):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        label: action.payload.label,
        isPristineLabel: false,
      });
    case getType(retagQuery.success):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        tags: action.payload.tags,
      });
    case getType(shareQuerySuccess):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        groups: action.payload.groups,
        ...(!action.payload.groups || action.payload.groups.length === 0
          ? { shared: false }
          : { shared: true }),
      });
    case getType(deleteQuerySuccess):
      return deletePreviousQuery(state, action.payload);
    case getType(loadQuery.failure):
    case getType(renameQuery.failure):
    case getType(retagQuery.failure):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: action.payload.message,
      });
    default:
      return state;
  }
};

export default previousQueriesReducer;
