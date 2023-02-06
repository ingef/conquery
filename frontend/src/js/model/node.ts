import { ConceptElementT, ConceptT } from "../api/types";
import { DNDType } from "../common/constants/dndTypes";
import type {
  ConceptQueryNodeType,
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import { objectHasNonDefaultSelects } from "./select";
import {
  tablesHaveNonDefaultSettings,
  tablesHaveEmptySettings,
  tablesHaveFilterValues,
} from "./table";

export interface NodeResetConfig {
  useDefaults?: boolean;
}

export const nodeIsConceptQueryNode = (
  node: StandardQueryNodeT,
): node is DragItemConceptTreeNode => node.type === DNDType.CONCEPT_TREE_NODE;

const nodeHasNonDefaultExludedTable = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return node.tables.some(
    (table) =>
      (table.exclude && table.default) || (!table.default && !table.exclude),
  );
};

export const nodeHasEmptySettings = (node: StandardQueryNodeT) => {
  return (
    !node.excludeFromSecondaryId &&
    !node.excludeTimestamps &&
    (!nodeIsConceptQueryNode(node) ||
      (tablesHaveEmptySettings(node.tables) &&
        (!node.selects || node.selects.every((select) => !select.selected))))
  );
};

export const nodeHasFilterValues = (node: StandardQueryNodeT) =>
  nodeIsConceptQueryNode(node) && tablesHaveFilterValues(node.tables);

export const nodeHasNonDefaultSettings = (node: StandardQueryNodeT) =>
  node.excludeTimestamps ||
  node.excludeFromSecondaryId ||
  (nodeIsConceptQueryNode(node) &&
    (objectHasNonDefaultSelects(node) ||
      nodeHasNonDefaultTableSettings(node) ||
      nodeHasNonDefaultExludedTable(node)));

export const nodeHasNonDefaultTableSettings = (node: ConceptQueryNodeType) => {
  if (!node.tables) return false;

  return tablesHaveNonDefaultSettings(node.tables);
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
