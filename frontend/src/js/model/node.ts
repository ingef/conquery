import {
  faFolderOpen as faFolderOpenRegular,
  faFolder as faFolderRegular,
} from "@fortawesome/free-regular-svg-icons";
import {
  faFolder,
  faFolderOpen,
  faMinus,
} from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import { ConceptElementT, ConceptT } from "../api/types";
import { DNDType } from "../common/constants/dndTypes";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import type {
  ConceptQueryNodeType,
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import { PossibleDroppableObject } from "../ui-components/Dropzone";

import { objectHasNonDefaultSelects } from "./select";
import {
  tablesHaveEmptySettings,
  tablesHaveFilterValues,
  tablesHaveNonDefaultSettings,
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

const nodeHasNonDefaultExcludeTimestamps = (node: StandardQueryNodeT) => {
  if (!nodeIsConceptQueryNode(node)) return node.excludeTimestamps;

  const root = getConceptById(node.tree, node.tree);

  return node.excludeTimestamps !== root?.excludeFromTimeAggregation;
};

export const nodeHasNonDefaultSettings = (node: StandardQueryNodeT) =>
  nodeHasNonDefaultExcludeTimestamps(node) ||
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

export function getNodeIcon(
  node: ConceptT,
  config?: {
    isStructNode?: boolean;
    open?: boolean;
  },
) {
  const hasChildren = node.children && node.children?.length > 0;

  if (!hasChildren) {
    return faMinus;
  }

  if (config?.open) {
    return config?.isStructNode ? faFolderOpenRegular : faFolderOpen;
  }

  return config?.isStructNode ? faFolderRegular : faFolder;
}

const droppableObjectIsConceptTreeNode = (
  node: PossibleDroppableObject,
): node is DragItemConceptTreeNode => {
  return node.type === DNDType.CONCEPT_TREE_NODE;
};

export const canNodeBeDropped = (
  node: StandardQueryNodeT,
  item: PossibleDroppableObject,
) => {
  if (
    !droppableObjectIsConceptTreeNode(item) ||
    !nodeIsConceptQueryNode(node)
  ) {
    return false;
  }
  const conceptId = item.ids[0];
  const itemAlreadyInNode = node.ids.includes(conceptId);
  const itemHasConceptRoot = item.tree === node.tree;
  return itemHasConceptRoot && !itemAlreadyInNode;
};

export const useActiveState = (node?: StandardQueryNodeT) => {
  const { t } = useTranslation();

  if (!node) {
    return {
      active: false,
      tooltipText: undefined,
    };
  }

  const hasNonDefaultSettings = !node.error && nodeHasNonDefaultSettings(node);
  const hasFilterValues = nodeHasFilterValues(node);

  const tooltipText = hasNonDefaultSettings
    ? t("queryEditor.hasNonDefaultSettings")
    : hasFilterValues
    ? t("queryEditor.hasDefaultSettings")
    : undefined;

  return {
    active: hasNonDefaultSettings || hasFilterValues,
    tooltipText,
  };
};
