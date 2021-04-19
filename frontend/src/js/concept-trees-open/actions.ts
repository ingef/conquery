import type { ConceptIdT } from "../api/types";

import {
  SET_CONCEPT_OPEN,
  RESET_ALL_CONCEPT_OPEN,
  CLOSE_ALL_CONCEPT_OPEN,
} from "./actionTypes";

export function setConceptOpen(conceptId: ConceptIdT, open: boolean) {
  return {
    type: SET_CONCEPT_OPEN,
    payload: {
      conceptId,
      open,
    },
  };
}

export function resetAllConceptOpen() {
  return {
    type: RESET_ALL_CONCEPT_OPEN,
  };
}

export function closeAllConceptOpen(rootConceptIds: ConceptIdT[]) {
  return {
    type: CLOSE_ALL_CONCEPT_OPEN,
    payload: {
      rootConceptIds,
    },
  };
}
