import { useSelector } from "react-redux";

import type { ConceptIdT } from "../api/types";
import type { StateT } from "../app/reducers";

import type { SearchT, TreesT } from "./reducer";

const isChildWithinResults = (children: ConceptIdT[], search: SearchT) => {
  return children.some(
    (child) =>
      !!search.result && Object.hasOwnProperty.call(search.result, child),
  );
};

export const isNodeInSearchResult = (
  id: ConceptIdT,
  search: SearchT,
  children?: ConceptIdT[],
) => {
  if (!search.result) return true;

  if (Object.hasOwnProperty.call(search.result, id)) return true;

  if (!!children && children.length > 0)
    return isChildWithinResults(children, search);

  return false;
};

export const useAreTreesAvailable = () => {
  const trees = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  return !!trees && Object.keys(trees).length > 0;
};
