import { ActionType, createAction } from "typesafe-actions";

import type { ProjectItemsFilterStateT } from "./reducer";

export type ProjectItemsFilterActions = ActionType<typeof setFilter>;

export const setFilter = createAction(
  "project-items/SET_FILTER",
)<ProjectItemsFilterStateT>();
