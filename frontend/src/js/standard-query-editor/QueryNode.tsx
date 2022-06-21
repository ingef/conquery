import styled from "@emotion/styled";
import { useRef } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { QueryT } from "../api/types";
import { getWidthAndHeight } from "../app/DndProvider";
import type { StateT } from "../app/reducers";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import ErrorMessage from "../error-message/ErrorMessage";
import {
  nodeHasNonDefaultSettings,
  nodeHasFilterValues,
  nodeIsConceptQueryNode,
} from "../model/node";
import { isQueryExpandable } from "../model/query";
import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";
import WithTooltip from "../tooltip/WithTooltip";

import QueryNodeActions from "./QueryNodeActions";
import { getRootNodeLabel } from "./helper";
import { StandardQueryNodeT } from "./types";

const Root = styled("div")<{
  active?: boolean;
}>`
  position: relative;
  width: 100%;
  margin: 0 auto;
  background-color: white;
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

const Node = styled("div")`
  flex-grow: 1;
  padding-top: 2px;
`;

const Label = styled("p")`
  margin: 0;
  word-break: break-word;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.md};
`;
const Description = styled("p")`
  margin: 3px 0 0;
  word-break: break-word;
  line-height: 1.2;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
`;

const PreviousQueryLabel = styled("p")`
  margin: 0 0 3px;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const StyledErrorMessage = styled(ErrorMessage)`
  margin: 0;
`;

const RootNode = styled("p")`
  margin: 0 0 4px;
  line-height: 1;
  text-transform: uppercase;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  word-break: break-word;
`;

interface PropsT {
  node: StandardQueryNodeT;
  andIdx: number;
  orIdx: number;
  onDeleteNode: () => void;
  onEditClick: () => void;
  onToggleTimestamps: () => void;
  onToggleSecondaryIdExclude: () => void;
  onExpandClick: (q: QueryT) => void;
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

  const QueryNodeRoot = (
    <Root
      ref={(instance) => {
        ref.current = instance;
        drag(instance);
      }}
      active={hasNonDefaultSettings || hasFilterValues}
      onClick={!!node.error ? () => {} : onEditClick}
    >
      <WithTooltip text={tooltipText}>
        <Node>
          {!nodeIsConceptQueryNode(node) && (
            <PreviousQueryLabel>
              {t("queryEditor.previousQuery")}
            </PreviousQueryLabel>
          )}
          {node.error ? (
            <StyledErrorMessage message={node.error} />
          ) : (
            <>
              {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
              <Label>
                {node.label || (!nodeIsConceptQueryNode(node) && node.id)}
              </Label>
              {nodeIsConceptQueryNode(node) &&
                (!node.ids || node.ids.length === 1) && (
                  <Description>{node.description}</Description>
                )}
            </>
          )}
        </Node>
      </WithTooltip>
      <QueryNodeActions
        excludeTimestamps={node.excludeTimestamps}
        onDeleteNode={onDeleteNode}
        onToggleTimestamps={onToggleTimestamps}
        isExpandable={isQueryExpandable(node)}
        hasActiveSecondaryId={hasActiveSecondaryId}
        excludeFromSecondaryId={node.excludeFromSecondaryId}
        onToggleSecondaryIdExclude={onToggleSecondaryIdExclude}
        onExpandClick={() => {
          if (nodeIsConceptQueryNode(node) || !node.query) return;

          onExpandClick(node.query);
        }}
        previousQueryLoading={node.loading}
        error={node.error}
      />
    </Root>
  );

  if (nodeIsConceptQueryNode(node)) {
    const conceptById = getConceptById(node.ids[0]);

    if (conceptById) {
      return (
        <AdditionalInfoHoverable node={conceptById}>
          {QueryNodeRoot}
        </AdditionalInfoHoverable>
      );
    }
  }

  return QueryNodeRoot;
};

export default QueryNode;
