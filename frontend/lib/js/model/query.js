// @flow

import type { ConceptQueryNodeType } from "../standard-query-editor/types";
import { TIMEBASED_OPERATOR_TYPES } from "../common/constants/timebasedQueryOperatorTypes";

function isTimebasedQuery(node) {
  const queryString = JSON.stringify(node.query);

  Object.values(TIMEBASED_OPERATOR_TYPES).some(
    op => queryString.indexOf(op) === -1
  );
}

// A little weird that it's nested so deeply, but well, you can't expand an external query
function isExternalQuery(node) {
  return (
    node.query.type === "CONCEPT_QUERY" &&
    node.query.root &&
    node.query.root.type === "EXTERNAL_RESOLVED"
  );
}

export function isQueryExpandable(node: ConceptQueryNodeType) {
  if (!node.isPreviousQuery || !node.query) return false;

  return !isTimebasedQuery(node) && !isExternalQuery(node);
}
