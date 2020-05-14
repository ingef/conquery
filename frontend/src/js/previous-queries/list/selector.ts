import { PreviousQueryT } from "./reducer";

const queryHasTag = (query: PreviousQueryT, searchTerm: string) => {
  return (
    !!query.tags &&
    query.tags.some(tag => {
      return tag.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1;
    })
  );
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
  search: string[],
  filter: string
) => {
  if (search.length === 0 && filter === "all") return queries;

  return queries.filter(query => {
    return (
      queryHasFilterType(query, filter) &&
      search.every(searchTerm => {
        return (
          queryHasId(query, searchTerm) ||
          queryHasLabel(query, searchTerm) ||
          queryHasTag(query, searchTerm)
        );
      })
    );
  });
};
