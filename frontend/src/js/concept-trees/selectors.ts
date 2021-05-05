import type { ConceptIdT } from "../api/types";

import type { SearchT } from "./reducer";

const isChildWithinResults = (children: [], search: SearchT) => {
  return children.some((child) => search.result.hasOwnProperty(child));
};

export const isNodeInSearchResult = (
  id: ConceptIdT,
  children?: [],
  search: SearchT,
) => {
  if (!search.result) return true;

  if (search.result.hasOwnProperty(id)) return true;

  if (!!children && children.length > 0)
    return isChildWithinResults(children, search);

  return false;
};

export const getAreTreesAvailable = (state) => {
  return (
    !!state.conceptTrees.trees &&
    Object.keys(state.conceptTrees.trees).length > 0
  );
};
