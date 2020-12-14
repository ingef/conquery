import React, { useRef, FC } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { useDrag } from "react-dnd";

import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";
import { QUERY_NODE } from "../common/constants/dndTypes";
import ErrorMessage from "../error-message/ErrorMessage";
import { nodeHasActiveFilters } from "../model/node";
import { isQueryExpandable } from "../model/query";
import { getWidthAndHeight } from "../app/DndProvider";

import QueryNodeActions from "./QueryNodeActions";

import { getRootNodeLabel } from "./helper";
import type { QueryNodeType, DraggedNodeType, DraggedQueryType } from "./types";

const Root = styled("div")<{ hasActiveFilters: boolean }>`
  position: relative;
  width: 100%;
  margin: 0 auto;
  background-color: white;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 7px;
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;
  text-align: left;
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: border ${({ theme }) => theme.transitionTime};
  border: ${({ theme, hasActiveFilters }) =>
    hasActiveFilters
      ? `2px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.grayMediumLight}`};
  &:hover {
    border: ${({ theme, hasActiveFilters }) =>
      hasActiveFilters
        ? `2px solid ${theme.col.blueGrayDark}`
        : `1px solid ${theme.col.blueGrayDark}`};
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
  node: QueryNodeType;
  onDeleteNode: Function;
  onEditClick: Function;
  onToggleTimestamps: Function;
  onExpandClick: Function;
  andIdx: number;
  orIdx: number;
}

const QueryNode: FC<PropsT> = ({
  node,
  andIdx,
  orIdx,
  onExpandClick,
  onEditClick,
  onDeleteNode,
  onToggleTimestamps,
}) => {
  const hasActiveFilters = !node.error && nodeHasActiveFilters(node);
  const rootNodeLabel = getRootNodeLabel(node);
  const ref = useRef<HTMLDivElement | null>(null);

  const item = {
    // Return the data describing the dragged item
    // NOT using `...node` since that would also spread `children` in.
    // This item may stem from either:
    // 1) A concept (dragged from ConceptTreeNode)
    // 2) A previous query (dragged from PreviousQueries)

    moved: true,
    andIdx,
    orIdx,
    type: QUERY_NODE,

    label: node.label,
    excludeTimestamps: node.excludeTimestamps,

    additionalInfos: node.additionalInfos,
    matchingEntries: node.matchingEntries,
    dateRange: node.dateRange,

    loading: node.loading,
    error: node.error,
    ...(node.isPreviousQuery
      ? {
          id: node.id,
          query: node.query,
          isPreviousQuery: true,
        }
      : {
          ids: node.ids,
          description: node.description,
          tree: node.tree,
          tables: node.tables,
          selects: node.selects,
        }),
  };
  const [, drag] = useDrag({
    item,
    begin: (): DraggedNodeType | DraggedQueryType => ({
      ...item,
      ...getWidthAndHeight(ref),
    }),
  });

  return (
    <Root
      ref={(instance) => {
        ref.current = instance;
        drag(instance);
      }}
      hasActiveFilters={hasActiveFilters}
      onClick={!!node.error ? () => null : onEditClick}
    >
      <Node>
        {node.isPreviousQuery && (
          <PreviousQueryLabel>
            {T.translate("queryEditor.previousQuery")}
          </PreviousQueryLabel>
        )}
        {node.error ? (
          <StyledErrorMessage message={node.error} />
        ) : (
          <>
            {rootNodeLabel && <RootNode>{rootNodeLabel}</RootNode>}
            <Label>{node.label || node.id}</Label>
            {node.description && (!node.ids || node.ids.length === 1) && (
              <Description>{node.description}</Description>
            )}
          </>
        )}
      </Node>
      <QueryNodeActions
        excludeTimestamps={node.excludeTimestamps}
        onEditClick={onEditClick}
        onDeleteNode={onDeleteNode}
        onToggleTimestamps={onToggleTimestamps}
        isExpandable={isQueryExpandable(node)}
        onExpandClick={() => {
          if (!node.query) return;

          onExpandClick(node.query);
        }}
        previousQueryLoading={node.loading}
        error={node.error}
      />
    </Root>
  );
};

export default AdditionalInfoHoverable(QueryNode);
