import { getType } from "typesafe-actions";

import { Action } from "../app/actions";
import { searchTrees } from "../concept-trees/actions";

import {
  closeAllConceptOpen,
  resetAllConceptOpen,
  setConceptOpen,
} from "./actions";

export type ConceptTreesOpenStateT = {
  [conceptId: string]: boolean;
};

const initialState: ConceptTreesOpenStateT = {};

const conceptTreesOpen = (
  state: ConceptTreesOpenStateT = initialState,
  action: Action,
): ConceptTreesOpenStateT => {
  switch (action.type) {
    case getType(setConceptOpen): {
      const { conceptId, open } = action.payload;

      return { ...state, [conceptId]: open };
    }
    case getType(closeAllConceptOpen):
      return action.payload.rootConceptIds.reduce((all, conceptId) => {
        all[conceptId] = false;
        return all;
      }, {});
    case getType(searchTrees.success):
    case getType(resetAllConceptOpen):
      return initialState;
    default:
      return state;
  }
};

export default conceptTreesOpen;
