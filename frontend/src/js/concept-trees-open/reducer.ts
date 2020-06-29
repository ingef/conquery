import {
  RESET_ALL_CONCEPT_OPEN,
  SET_CONCEPT_OPEN,
  CLOSE_ALL_CONCEPT_OPEN,
} from "./actionTypes";
import { SEARCH_TREES_SUCCESS } from "../concept-trees/actionTypes";

export type ConceptTreesOpenStateT = {
  [conceptId: string]: boolean;
};

const initialState: ConceptTreesOpenStateT = {};

const conceptTreesOpen = (
  state: ConceptTreesOpenStateT = initialState,
  action: Object
): ConceptTreesOpenStateT => {
  switch (action.type) {
    case SET_CONCEPT_OPEN: {
      const { conceptId, open } = action.payload;

      return { ...state, [conceptId]: open };
    }
    case CLOSE_ALL_CONCEPT_OPEN:
      return action.payload.rootConceptIds.reduce((all, conceptId) => {
        all[conceptId] = false;
        return all;
      }, {});
    case SEARCH_TREES_SUCCESS:
    case RESET_ALL_CONCEPT_OPEN:
      return initialState;
    default:
      return state;
  }
};

export default conceptTreesOpen;
