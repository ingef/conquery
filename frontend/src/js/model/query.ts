import { exists } from "../common/helpers/exists";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import type {
  PreviousQueryQueryNodeType,
  QueryGroupType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import { TIMEBASED_OPERATOR_TYPES } from "../timebased-query-editor/reducer";

import { nodeIsConceptQueryNode } from "./node";

function isTimebasedQuery(node: PreviousQueryQueryNodeType) {
  if (!node.query) return false;

  const queryString = JSON.stringify(node.query);

  return TIMEBASED_OPERATOR_TYPES.some((op) => queryString.indexOf(op) !== -1);
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

function elementHasValidDates(element: StandardQueryNodeT) {
  return !element.excludeTimestamps;
}

function groupHasValidDates(group: QueryGroupType) {
  return !group.exclude && group.elements.some(elementHasValidDates);
}

export function validateQueryDates(query: StandardQueryStateT) {
  return !query || query.length === 0 || query.some(groupHasValidDates);
}
