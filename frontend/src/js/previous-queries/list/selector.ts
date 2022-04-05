import { StateT } from "app-types";
import { useMemo } from "react";
import { useSelector } from "react-redux";

import { exists } from "../../common/helpers/exists";
import { configHasFilterType } from "../../external-forms/form-configs/selectors";
import type { ProjectItemsFilterStateT } from "../filter/reducer";

import type { FormConfigT, PreviousQueryT } from "./reducer";

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

const queryMatchesFolderFilter = (
  query: PreviousQueryT,
  folders: string[],
  noFoldersActive: boolean,
) => {
  return noFoldersActive
    ? query.tags.length === 0
    : folders.every((folder) => queryHasFolder(query, folder));
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
  filter: ProjectItemsFilterStateT,
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
  filter: ProjectItemsFilterStateT,
  folders: string[],
  noFoldersActive: boolean,
) => {
  const noFilterSet =
    (!exists(searchTerm) || searchTerm.length === 0) &&
    filter === "all" &&
    folders.length === 0 &&
    !noFoldersActive;

  if (noFilterSet) {
    return queries;
  }

  return queries.filter(
    (query) =>
      queryHasFilterType(query, filter) &&
      queryMatchesFolderFilter(query, folders, noFoldersActive) &&
      queryMatchesSearch(query, searchTerm),
  );
};

export const useFolders = () => {
  const filter = useSelector<StateT, ProjectItemsFilterStateT>(
    (state) => state.projectItemsFilter,
  );
  const queries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const formConfigs = useSelector<StateT, FormConfigT[]>(
    (state) => state.previousQueries.formConfigs,
  );
  const localFolders = useSelector<StateT, string[]>(
    (state) => state.previousQueries.localFolders,
  );

  return useMemo(
    () =>
      Array.from(
        new Set([
          ...queries
            .filter((query) => queryHasFilterType(query, filter))
            .flatMap((query) => query.tags),
          ...formConfigs
            .filter((config) => configHasFilterType(config, filter))
            .flatMap((config) => config.tags),
          ...localFolders,
        ]),
      ).sort(),
    [queries, formConfigs, localFolders, filter],
  );
};
