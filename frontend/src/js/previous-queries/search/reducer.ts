import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { clearSearch, setSearch } from "./actions";

export interface ProjectItemsSearchStateT {
  searchTerm: string | null;
  result: Record<string, number> | null;
  words: string[];
}

const initialState: ProjectItemsSearchStateT = {
  searchTerm: null,
  result: null,
  words: [],
};

const projectItemsSearch = (
  state: ProjectItemsSearchStateT = initialState,
  action: Action,
): ProjectItemsSearchStateT => {
  switch (action.type) {
    case getType(setSearch):
      return { ...state, ...action.payload };
    case getType(clearSearch):
      return initialState;
    default:
      return state;
  }
};

export default projectItemsSearch;
