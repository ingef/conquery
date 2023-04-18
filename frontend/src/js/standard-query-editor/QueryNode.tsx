import styled from "@emotion/styled";
import { memo, useCallback, useRef } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { QueryT } from "../api/types";
import { getWidthAndHeight } from "../app/DndProvider";
import type { StateT } from "../app/reducers";
import { DNDType } from "../common/constants/dndTypes";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import {
  nodeHasNonDefaultSettings,
  nodeHasFilterValues,
  nodeIsConceptQueryNode,
} from "../model/node";
import { isQueryExpandable } from "../model/query";
import { HoverNavigatable } from "../small-tab-navigation/HoverNavigatable";
import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";
import { PossibleDroppableObject } from "../ui-components/Dropzone";

import QueryNodeActions from "./QueryNodeActions";
import QueryNodeContent from "./QueryNodeContent";
import { getRootNodeLabel } from "./helper";
import { DragItemConceptTreeNode, StandardQueryNodeT } from "./types";

const FlexHoverNavigatable = styled(HoverNavigatable)`
  display: flex;
  width: 100%;
`;

const Root = styled("div")<{
  active?: boolean;
}>`
  position: relative;
  width: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr auto;

  padding: 7px;
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;
  text-align: left;
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: background-color ${({ theme }) => theme.transitionTime};
  border: ${({ theme, active }) =>
    active
      ? `2px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.grayMediumLight}`};
  &:hover {
    background-color: ${({ theme }) => theme.col.bg};
  }
`;

interface PropsT {
  node: StandardQueryNodeT;
  andIdx: number;
  orIdx: number;
  onDeleteNode: (andIdx: number, orIdx: number) => void;
  onEditClick: (andIdx: number, orIdx: number) => void;
  onExpandClick: (q: QueryT) => void;
  onToggleTimestamps: (andIdx: number, orIdx: number) => void;
  onToggleSecondaryIdExclude: (andIdx: number, orIdx: number) => void;
}

const nodeHasActiveSecondaryId = (
  node: StandardQueryNodeT,
  activeSecondaryId: string | null,
) => {
  if (!activeSecondaryId) {
    return false;
  }

  if (nodeIsConceptQueryNode(node)) {
    return node.tables.some(
      (table) =>
        !table.exclude &&
        table.supportedSecondaryIds &&
        table.supportedSecondaryIds.includes(activeSecondaryId),
    );
  } else {
    return (
      !!node.availableSecondaryIds &&
      node.availableSecondaryIds.includes(activeSecondaryId)
    );
  }
};

export const droppableObjectIsConceptTreeNode = (
  node: PossibleDroppableObject,
): node is DragItemConceptTreeNode => {
  return node.type === DNDType.CONCEPT_TREE_NODE;
};

const QueryNode = ({
  node,
  andIdx,
  orIdx,
  onExpandClick,
  onEditClick,
  onDeleteNode,
  onToggleTimestamps,
  onToggleSecondaryIdExclude,
}: PropsT) => {
  const { t } = useTranslation();
  const rootNodeLabel = getRootNodeLabel(node);
  const ref = useRef<HTMLDivElement | null>(null);

  const activeSecondaryId = useSelector<StateT, string | null>(
    (state) => state.queryEditor.selectedSecondaryId,
  );

  const hasNonDefaultSettings = !node.error && nodeHasNonDefaultSettings(node);
  const hasFilterValues = nodeHasFilterValues(node);
  const hasActiveSecondaryId = nodeHasActiveSecondaryId(
    node,
    activeSecondaryId,
  );

  const item: StandardQueryNodeT = {
    // Return the data describing the dragged item
    // NOT using `...node` since that would also spread `children` in.
    // This item may stem from either:
    // 1) A concept (dragged from ConceptTreeNode)
    // 2) A previous query (dragged from PreviousQueries)
    dragContext: {
      movedFromAndIdx: andIdx,
      movedFromOrIdx: orIdx,
      width: 0,
      height: 0,
    },

    label: node.label,
    excludeTimestamps: node.excludeTimestamps,
    excludeFromSecondaryId: node.excludeFromSecondaryId,

    loading: node.loading,
    error: node.error,

    ...(nodeIsConceptQueryNode(node)
      ? {
          ids: node.ids,
          type: node.type,
          description: node.description,
          tree: node.tree,
          tables: node.tables,
          selects: node.selects,

          additionalInfos: node.additionalInfos,
          matchingEntries: node.matchingEntries,
          matchingEntities: node.matchingEntities,
          dateRange: node.dateRange,
        }
      : {
          id: node.id,
          type: node.type,
          query: node.query,
          tags: node.tags,
        }),
  };
  const [, drag] = useDrag<StandardQueryNodeT, void, {}>({
    type: item.type,
    item: () =>
      ({
        ...item,
        dragContext: {
          ...item.dragContext,
          ...getWidthAndHeight(ref),
        },
      } as StandardQueryNodeT),
  });

  const tooltipText = hasNonDefaultSettings
    ? t("queryEditor.hasNonDefaultSettings")
    : hasFilterValues
    ? t("queryEditor.hasDefaultSettings")
    : undefined;

  const expandClick = useCallback(() => {
    if (nodeIsConceptQueryNode(node) || !node.query) return;

    onExpandClick(node.query);
  }, [onExpandClick, node]);

  const onClick = !!node.error ? () => {} : () => onEditClick(andIdx, orIdx);

  const label = nodeIsConceptQueryNode(node)
    ? node.label
    : node.label || node.id;

  const description =
    nodeIsConceptQueryNode(node) && (!node.ids || node.ids.length === 1)
      ? node.description
      : undefined;

  const QueryNodeRoot = (
    <FlexHoverNavigatable
      triggerNavigate={onClick}
      canDrop={(item: PossibleDroppableObject) => {
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
      }}
      highlightDroppable={true}
    >
      <Root
        ref={(instance) => {
          ref.current = instance;
          drag(instance);
        }}
        active={hasNonDefaultSettings || hasFilterValues}
        onClick={node.error ? undefined : () => onEditClick(andIdx, orIdx)}
      >
        <QueryNodeContent
          error={node.error}
          isConceptQueryNode={nodeIsConceptQueryNode(node)}
          tooltipText={tooltipText}
          label={label}
          description={description}
          rootNodeLabel={rootNodeLabel}
        />
        <QueryNodeActions
          andIdx={andIdx}
          orIdx={orIdx}
          excludeTimestamps={node.excludeTimestamps}
          isExpandable={isQueryExpandable(node)}
          hasActiveSecondaryId={hasActiveSecondaryId}
          excludeFromSecondaryId={node.excludeFromSecondaryId}
          onDeleteNode={onDeleteNode}
          onToggleTimestamps={onToggleTimestamps}
          onToggleSecondaryIdExclude={onToggleSecondaryIdExclude}
          onExpandClick={expandClick}
          previousQueryLoading={node.loading}
          error={node.error}
        />
      </Root>
    </FlexHoverNavigatable>
  );

  if (nodeIsConceptQueryNode(node)) {
    const conceptById = getConceptById(node.ids[0], node.tree);
    const root = getConceptById(node.tree, node.tree);

    if (conceptById && root) {
      return (
        <AdditionalInfoHoverable node={conceptById} root={root}>
          {QueryNodeRoot}
        </AdditionalInfoHoverable>
      );
    }
  }

  return QueryNodeRoot;
};

export default memo(QueryNode);
