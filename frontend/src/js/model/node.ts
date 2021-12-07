import { ConceptElementT, ConceptT } from "../api/types";
import type {
  ConceptQueryNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import { objectHasSelectedSelects } from "./select";
import { tablesHaveActiveFilter } from "./table";

export interface NodeResetConfig {
  useDefaults?: boolean;
}

export const nodeIsConceptQueryNode = (
  node: StandardQueryNodeT,
): node is ConceptQueryNodeType => !node.isPreviousQuery;

export const nodeHasActiveFilters = (node: StandardQueryNodeT) =>
  node.excludeTimestamps ||
  node.excludeFromSecondaryId ||
  (nodeIsConceptQueryNode(node) &&
    (node.includeSubnodes || // TODO REFACTOR / TYPE THIS ONE
      objectHasSelectedSelects(node) ||
      nodeHasActiveTableFilters(node) ||
      nodeHasExludedTable(node)));

export const nodeHasActiveTableFilters = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return tablesHaveActiveFilter(node.tables);
};

export const nodeHasExludedTable = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return node.tables.some((table) => table.exclude);
};

export function nodeIsInvalid(
  node: ConceptQueryNodeType,
  blocklistedConceptIds?: string[],
  allowlistedConceptIds?: string[],
) {
  return (
    (!!allowlistedConceptIds &&
      !nodeIsAllowlisted(node, allowlistedConceptIds)) ||
    (!!blocklistedConceptIds && nodeIsBlocklisted(node, blocklistedConceptIds))
  );
}

export function nodeIsBlocklisted(
  node: ConceptQueryNodeType,
  blocklistedConceptIds: string[],
) {
  return (
    !!node.ids &&
    blocklistedConceptIds.some((id) =>
      node.ids.some((conceptId) => conceptId.indexOf(id.toLowerCase()) !== -1),
    )
  );
}

export function nodeIsAllowlisted(
  node: ConceptQueryNodeType,
  allowlistedConceptIds: string[],
) {
  return (
    !!node.ids &&
    allowlistedConceptIds.some((id) =>
      node.ids.every((conceptId) => conceptId.indexOf(id.toLowerCase()) !== -1),
    )
  );
}

export function nodeIsElement(node: ConceptT): node is ConceptElementT {
  return "tables" in node;
}
