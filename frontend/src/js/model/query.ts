import type {
  ConceptQueryNodeType,
  StandardQueryType
} from "../standard-query-editor/types";
import { TIMEBASED_OPERATOR_TYPES } from "../common/constants/timebasedQueryOperatorTypes";

function isTimebasedQuery(node) {
  const queryString = JSON.stringify(node.query);

  return Object.values(TIMEBASED_OPERATOR_TYPES).some(
    op => queryString.indexOf(op) !== -1
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

// Validation

export function validateQueryLength(query: StandardQueryType) {
  return query.length > 0;
}

function elementHasValidDates(element) {
  return !element.excludeTimestamps;
}

function groupHasValidDates(group) {
  return !group.exclude && group.elements.some(elementHasValidDates);
}

export function validateQueryDates(query: StandardQueryType) {
  return !query || query.length === 0 || query.some(groupHasValidDates);
}
