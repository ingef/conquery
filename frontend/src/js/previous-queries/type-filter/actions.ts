import { ActionType, createAction } from "typesafe-actions";

import type { ProjectItemsTypeFilterStateT } from "./reducer";

export type ProjectItemsTypeFilterActions = ActionType<typeof setTypeFilter>;

export const setTypeFilter = createAction(
  "project-items/SET_TYPE_FILTER",
)<ProjectItemsTypeFilterStateT>();
