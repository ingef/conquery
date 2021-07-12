import { StateT } from "app-types";
import { useMemo } from "react";
import { useSelector } from "react-redux";

import { exists } from "../../common/helpers/exists";
import type { PreviousQueriesFilterStateT } from "../filter/reducer";

import type { PreviousQueryT } from "./reducer";

const queryHasTag = (query: PreviousQueryT, searchTerm: string) => {
  return (
    !!query.tags &&
    query.tags.some((tag) => {
      return tag.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1;
    })
  );
};

const queryHasFolder = (query: PreviousQueryT, folder: string) => {
  return !!query.tags && query.tags.some((tag) => tag === folder);
};

const queryHasLabel = (query: PreviousQueryT, searchTerm: string) => {
  return (
    query.label &&
    query.label.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1
  );
};

const queryHasId = (query: PreviousQueryT, searchTerm: string) => {
  return query.id.toString() === searchTerm;
};

export const queryHasFilterType = (
  query: PreviousQueryT,
  filter: PreviousQueriesFilterStateT,
) => {
  if (filter === "all") return true;

  // Checks query.own, query.shared or query.system
  if (query[filter]) return true;

  // Special case for a "system"-previous-query:
  // it's simply not shared and not self-created (own)
  if (filter === "system" && !query.shared && !query.own) return true;

  return false;
};

export const queryMatchesSearch = (
  query: PreviousQueryT,
  searchTerm: string | null,
) => {
  return (
    !exists(searchTerm) ||
    queryHasId(query, searchTerm) ||
    queryHasLabel(query, searchTerm) ||
    queryHasTag(query, searchTerm)
  );
};

export const selectPreviousQueries = (
  queries: PreviousQueryT[],
  searchTerm: string | null,
  filter: PreviousQueriesFilterStateT,
  folderFilter: string[],
  noFoldersActive: boolean,
) => {
  if (
    !exists(searchTerm) &&
    filter === "all" &&
    folderFilter.length === 0 &&
    !noFoldersActive
  )
    return queries;

  return queries.filter((query) => {
    const matchesFilter = queryHasFilterType(query, filter);
    const matchesFolderFilter = noFoldersActive
      ? query.tags.length === 0
      : folderFilter.every((folder) => queryHasFolder(query, folder));
    const matchesSearch = queryMatchesSearch(query, searchTerm);

    return matchesFilter && matchesFolderFilter && matchesSearch;
  });
};

export const usePreviousQueriesTags = () => {
  const queries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );

  return useMemo(
    () => Array.from(new Set(queries.flatMap((query) => query.tags))).sort(),
    [queries],
  );
};
