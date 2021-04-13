import type {
  ConceptQueryNodeType,
  PreviousQueryQueryNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import { TIMEBASED_OPERATOR_TYPES } from "../common/constants/timebasedQueryOperatorTypes";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";

function isTimebasedQuery(node: PreviousQueryQueryNodeType) {
  const queryString = JSON.stringify(node.query);

  return Object.values(TIMEBASED_OPERATOR_TYPES).some(
    (op) => queryString.indexOf(op) !== -1
  );
}

// A little weird that it's nested so deeply, but well, you can't expand an external query
function isExternalQuery(node: PreviousQueryQueryNodeType) {
  return (
    node.query.type === "CONCEPT_QUERY" &&
    node.query.root &&
    node.query.root.type === "EXTERNAL_RESOLVED"
  );
}

export function isQueryExpandable(node: StandardQueryNodeT) {
  if (!node.isPreviousQuery || !node.query || !node.canExpand) return false;

  return !isTimebasedQuery(node) && !isExternalQuery(node);
}

// Validation

export function validateQueryLength(query: StandardQueryStateT) {
  return query.length > 0;
}

function elementHasValidDates(element) {
  return !element.excludeTimestamps;
}

function groupHasValidDates(group) {
  return !group.exclude && group.elements.some(elementHasValidDates);
}

export function validateQueryDates(query: StandardQueryStateT) {
  return !query || query.length === 0 || query.some(groupHasValidDates);
}

export function isConceptQueryNode(
  node: StandardQueryNodeT
): node is ConceptQueryNodeType {
  return !node.isPreviousQuery;
}
