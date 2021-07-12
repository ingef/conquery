import { exists } from "../../common/helpers/exists";

import { PreviousQueryT } from "./reducer";

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

const queryHasFilterType = (query: PreviousQueryT, filter: string) => {
  if (filter === "all") return true;

  // Checks query.own, query.shared or query.system
  if (query[filter]) return true;

  // Special case for a "system"-previous-query:
  // it's simply not shared and not self-created (own)
  if (filter === "system" && !query.shared && !query.own) return true;

  return false;
};

export const selectPreviousQueries = (
  queries: PreviousQueryT[],
  search: string | null,
  filter: string,
  folderFilter: string[],
  noFoldersActive: boolean,
) => {
  if (
    !exists(search) &&
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
    const matchesSearch =
      !exists(search) ||
      queryHasId(query, search) ||
      queryHasLabel(query, search) ||
      queryHasTag(query, search);

    return matchesFilter && matchesFolderFilter && matchesSearch;
  });
};
