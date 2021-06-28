import { getType } from "typesafe-actions";

import type { QueryIdT, UserGroupIdT } from "../../api/types";
import { Action } from "../../app/actions";

import {
  deletePreviousQuerySuccess,
  loadPreviousQueries,
  loadPreviousQuery,
  renamePreviousQuery,
  retagPreviousQuery,
  sharePreviousQuerySuccess,
} from "./actions";

export type PreviousQueryIdT = string;
export interface PreviousQueryT {
  id: PreviousQueryIdT;
  label: string;
  loading?: boolean;
  error?: string | null;
  numberOfResults: number;
  createdAt: string;
  tags: string[];
  own: boolean;
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
  tags: string[];
  names: string[];
  error: string | null;
}

const initialState: PreviousQueriesStateT = {
  queries: [],
  loading: false,
  tags: [],
  names: [],
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

const findUniqueTags = (queries: PreviousQueryT[]) => {
  const uniqueTags = new Set<string>();

  queries.forEach((query) => {
    if (query.tags) query.tags.forEach((tag) => uniqueTags.add(tag));
  });

  return Array.from(uniqueTags);
};

const findNewTags = (tags: string[]) => {
  if (!tags) return [];

  let uniqueTags = new Set<string>();

  tags.forEach((tag) => uniqueTags.add(tag));

  return Array.from(uniqueTags);
};

const findUniqueNames = (queries: PreviousQueryT[]) => {
  const uniqueNames = new Set<string>();

  queries.filter((q) => !!q.label).forEach((q) => uniqueNames.add(q.label));

  return Array.from(uniqueNames);
};

const updateUniqueNames = (existingNames: string[], newName: string) => {
  return existingNames.includes(newName)
    ? existingNames
    : [newName, ...existingNames];
};

const previousQueriesReducer = (
  state: PreviousQueriesStateT = initialState,
  action: Action,
): PreviousQueriesStateT => {
  switch (action.type) {
    case getType(loadPreviousQueries.request):
      return { ...state, loading: true };
    case getType(loadPreviousQueries.success):
      return {
        ...state,
        loading: false,
        queries: sortQueries(action.payload.data),
        tags: findUniqueTags(action.payload.data),
        names: findUniqueNames(action.payload.data),
      };
    case getType(loadPreviousQueries.failure):
      return {
        ...state,
        loading: false,
        error: action.payload.message || null,
      };
    case getType(loadPreviousQuery.request):
    case getType(renamePreviousQuery.request):
    case getType(retagPreviousQuery.request):
      return updatePreviousQuery(state, action, { loading: true });
    case getType(loadPreviousQuery.success):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        ...action.payload.data,
      });
    case getType(renamePreviousQuery.success):
      return {
        ...updatePreviousQuery(state, action, {
          loading: false,
          error: null,
          label: action.payload.label,
          isPristineLabel: false,
        }),
        names: updateUniqueNames(state.names, action.payload.label),
      };
    case getType(retagPreviousQuery.success):
      return {
        ...updatePreviousQuery(state, action, {
          loading: false,
          error: null,
          tags: action.payload.tags,
        }),
        tags: findNewTags([...state.tags, ...action.payload.tags]),
      };
    case getType(sharePreviousQuerySuccess):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        groups: action.payload.groups,
        ...(!action.payload.groups || action.payload.groups.length === 0
          ? { shared: false }
          : { shared: true }),
      });
    case getType(deletePreviousQuerySuccess):
      return deletePreviousQuery(state, action.payload);
    case getType(loadPreviousQuery.failure):
    case getType(renamePreviousQuery.failure):
    case getType(retagPreviousQuery.failure):
      return updatePreviousQuery(state, action, {
        loading: false,
        error: action.payload.message,
      });
    default:
      return state;
  }
};

export default previousQueriesReducer;
