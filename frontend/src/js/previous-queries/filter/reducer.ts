import { getType } from "typesafe-actions";

import { Action } from "../../app/actions";

import { setFilter } from "./actions";

export type ProjectItemsFilterStateT = "all" | "own" | "shared" | "system";

const initialState: ProjectItemsFilterStateT = "own";

const projectItemsFilter = (
  state: ProjectItemsFilterStateT = initialState,
  action: Action,
): ProjectItemsFilterStateT => {
  switch (action.type) {
    case getType(setFilter):
      return action.payload;
    default:
      return state;
  }
};

export default projectItemsFilter;
