import { ActionType, createAction } from "typesafe-actions";

import type { ConceptIdT } from "../api/types";

export type ConceptTreesOpenActions = ActionType<
  | typeof setConceptOpen
  | typeof resetAllConceptOpen
  | typeof closeAllConceptOpen
>;

export const setConceptOpen = createAction("concept-trees-open/SET")<{
  conceptId: ConceptIdT;
  open: boolean;
}>();

export const resetAllConceptOpen = createAction(
  "concept-trees-open/RESET_ALL",
)();
export const closeAllConceptOpen = createAction(
  "concept-trees-open/CLOSE_ALL",
)<{ rootConceptIds: ConceptIdT[] }>();
