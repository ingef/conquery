// @flow

import type { ConceptQueryNodeType } from "../standard-query-editor/types";
import { TIMEBASED_OPERATOR_TYPES } from "../common/constants/timebasedQueryOperatorTypes";

export function isQueryExpandable(node: ConceptQueryNodeType) {
  if (!node.isPreviousQuery || !node.query) return false;

  const queryString = JSON.stringify(node.query);

  return Object.values(TIMEBASED_OPERATOR_TYPES).every(
    op => queryString.indexOf(op) === -1
  );
}
