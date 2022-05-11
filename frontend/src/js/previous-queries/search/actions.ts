import { useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { ActionType, createAction } from "typesafe-actions";

import type { StateT } from "../../app/reducers";
import { exists } from "../../common/helpers/exists";
import {
  configHasFilterType,
  configMatchesSearch,
} from "../../external-forms/form-configs/selectors";
import type { ProjectItemsFilterStateT } from "../filter/reducer";
import type { ProjectItemT } from "../list/ProjectItem";
import { isFormConfig } from "../list/helpers";
import type { FormConfigT, PreviousQueryT } from "../list/reducer";
import { queryHasFilterType, queryMatchesSearch } from "../list/selector";

import type { ProjectItemsSearchStateT } from "./reducer";

export type ProjectItemsSearchActions = ActionType<
  typeof setSearch | typeof clearSearch
>;

export const setSearch = createAction("project-items/SET_SEARCH")<
  Partial<ProjectItemsSearchStateT>
>();

export const clearSearch = createAction("project-items/CLEAR_SEARCH")();

function searchItems(term: string, items: ProjectItemT[]) {
  // Using "__" as prefix & suffix to avoid accidentally matching a folder called "all",
  // assuming noone names their folders with "__" prefix & suffix
  const result: Record<string, number> = {
    __all__: 0,
    __without_folder__: 0,
  };

  for (const item of items) {
    const matchesSearch = isFormConfig(item)
      ? configMatchesSearch(item, term)
      : queryMatchesSearch(item, term);

    if (!matchesSearch) continue;

    result.__all__ += 1;

    if (item.tags.length === 0) {
      result.__without_folder__ += 1;
    }

    for (const tag of item.tags) {
      if (!exists(result[tag])) result[tag] = 0;

      result[tag] += 1;
    }
  }

  return result;
}

export const useSearchItems = () => {
  const dispatch = useDispatch();
  const previousQueries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const formConfigs = useSelector<StateT, FormConfigT[]>(
    (state) => state.previousQueries.formConfigs,
  );
  const filter = useSelector<StateT, ProjectItemsFilterStateT>(
    (state) => state.projectItemsFilter,
  );
  const filteredItems = useMemo(
    () => [
      ...previousQueries.filter((query) => queryHasFilterType(query, filter)),
      ...formConfigs.filter((config) => configHasFilterType(config, filter)),
    ],
    [previousQueries, formConfigs, filter],
  );

  return (searchTerm: string) => {
    const result = searchItems(searchTerm, filteredItems);

    dispatch(setSearch({ searchTerm, result, words: searchTerm.split(" ") }));
  };
};
