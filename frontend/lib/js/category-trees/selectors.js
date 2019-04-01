// @flow

import { type TreeNodeIdType } from "../common/types/backend";
import { type SearchType } from "./reducer";

const isChildWithinResults = (children: [], search: SearchType) => {
  return children.some(child => search.result.hasOwnProperty(child));
};

export const isNodeInSearchResult = (
  id: TreeNodeIdType,
  children?: [],
  search: SearchType
) => {
  if (!search.result) return true;

  if (search.result.hasOwnProperty(id)) return true;

  if (!!children && children.length > 0)
    return isChildWithinResults(children, search);

  return false;
};
