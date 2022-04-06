import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { setTypeFilter } from "./actions";

export type ProjectItemsTypeFilterStateT = "all" | "queries" | "configs";

const initialState: ProjectItemsTypeFilterStateT = "all";

const projectItemsFilter = (
  state: ProjectItemsTypeFilterStateT = initialState,
  action: Action,
): ProjectItemsTypeFilterStateT => {
  switch (action.type) {
    case getType(setTypeFilter):
      return action.payload;
    default:
      return state;
  }
};

export default projectItemsFilter;
