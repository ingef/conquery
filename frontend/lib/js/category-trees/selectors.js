// @flow

import { includes } from "../common/helpers/commonHelper";

import { type TreeNodeIdType } from "../common/types/backend";
import { type SearchType } from "./reducer";

const isChildWithinResults = (children: [], search: SearchType) => {
  for (let child of children) {
    if (includes(search.result, child)) return true;
  }

  return false;
};

export const isNodeInSearchResult = (
  id: TreeNodeIdType,
  children?: [],
  search: SearchType
) => {
  if (!search.result) return true;

  if (search.result.includes(id)) return true;

  if (!!children && children.length > 0)
    return isChildWithinResults(children, search);

  return false;
};
