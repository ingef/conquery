// @flow

import { type ConceptIdT } from "../api/types";
import { type SearchType } from "./reducer";

const isChildWithinResults = (children: [], search: SearchType) => {
  return children.some(child => search.result.hasOwnProperty(child));
};

export const isNodeInSearchResult = (
  id: ConceptIdT,
  children?: [],
  search: SearchType
) => {
  if (!search.result) return true;

  if (search.result.hasOwnProperty(id)) return true;

  if (!!children && children.length > 0)
    return isChildWithinResults(children, search);

  return false;
};

export const getAreTreesAvailable = state => {
  return (
    state.conceptTrees.trees && Object.keys(state.conceptTrees.trees).length > 0
  );
};
