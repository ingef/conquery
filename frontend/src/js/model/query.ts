import { exists } from "../common/helpers/exists";
import { TIME_OPERATORS } from "../editor-v2/types";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import type {
  PreviousQueryQueryNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import { nodeIsConceptQueryNode } from "./node";

function isTimebasedQuery(node: PreviousQueryQueryNodeType) {
  if (!node.query) return false;

  const queryString = JSON.stringify(node.query);

  return TIME_OPERATORS.some((op) => queryString.indexOf(op) !== -1);
}

// A little weird that it's nested so deeply, but well, you can't expand an external query
function isExternalQuery(node: PreviousQueryQueryNodeType) {
  if (!node.query) return false;

  return (
    node.query.type === "CONCEPT_QUERY" &&
    node.query.root &&
    node.query.root.type === "EXTERNAL_RESOLVED"
  );
}

export function isQueryExpandable(node: StandardQueryNodeT) {
  if (nodeIsConceptQueryNode(node) || !node.canExpand) {
    return false;
  } else {
    return (
      exists(node.query) && !isTimebasedQuery(node) && !isExternalQuery(node)
    );
  }
}

// Validation

export function validateQueryLength(query: StandardQueryStateT) {
  return query.length > 0;
}
