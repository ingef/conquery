import { StateT } from "app-types";
import { useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction } from "typesafe-actions";

import { exists } from "../../common/helpers/exists";
import { PreviousQueriesFilterStateT } from "../filter/reducer";
import { PreviousQueryT } from "../list/reducer";
import { queryHasFilterType, queryMatchesSearch } from "../list/selector";

import type { QueriesSearchStateT } from "./reducer";

export type QueriesSearchActions = ActionType<
  typeof setQueriesSearch | typeof clearQueriesSearch
>;

export const setQueriesSearch = createAction(
  "previous-queries/SET_QUERIES_SEARCH",
)<Partial<QueriesSearchStateT>>();

export const clearQueriesSearch = createAction(
  "previous-queries/CLEAR_QUERIES_SEARCH",
)();

function searchQueries(term: string, queries: PreviousQueryT[]) {
  const result: Record<string, number> = {
    __all__: 0,
    __without_folder__: 0,
  };

  for (const query of queries) {
    const matchesSearch = queryMatchesSearch(query, term);

    if (!matchesSearch) continue;

    result.__all__ += 1;

    if (query.tags.length === 0) {
      result.__without_folder__ += 1;
    }

    for (const tag of query.tags) {
      if (!exists(result[tag])) result[tag] = 0;

      result[tag] += 1;
    }
  }

  return result;
}

export const useSearchQueries = () => {
  const dispatch = useDispatch();
  const previousQueries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const filter = useSelector<StateT, PreviousQueriesFilterStateT>(
    (state) => state.previousQueriesFilter,
  );
  const filteredQueries = useMemo(
    () => previousQueries.filter((query) => queryHasFilterType(query, filter)),
    [previousQueries, filter],
  );

  return (searchTerm: string) => {
    const result = searchQueries(searchTerm, filteredQueries);

    dispatch(
      setQueriesSearch({ searchTerm, result, words: searchTerm.split(" ") }),
    );
  };
};
