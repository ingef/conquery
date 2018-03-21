// @flow

import React                       from 'react';
import T                           from 'i18n-react';
import {
  DragSource,
  type ConnectDragSource
}                                  from 'react-dnd';

import { dndTypes }                from '../common/constants';
import { AdditionalInfoHoverable } from '../tooltip';
import { ErrorMessage }            from '../error-message';
import { nodeHasActiveFilters }    from '../model/node';

import QueryNodeActions            from './QueryNodeActions';

import type {
  QueryNodeType,
  DraggedNodeType,
  DraggedQueryType
}                                  from './types';

type PropsType =  {
  node: QueryNodeType,
  onDeleteNode: Function,
  onFilterClick: Function,
  onExpandClick: Function,
  connectDragSource: Function,
  andIdx: number,
  orIdx: number,
  connectDragSource: ConnectDragSource
};

const QueryNode = (props: PropsType) => {
  const { node } = props;

  return props.connectDragSource(
    <div className="query-node">
      <QueryNodeActions
        hasActiveFilters={nodeHasActiveFilters(node)}
        onFilterClick={props.onFilterClick}
        onDeleteNode={props.onDeleteNode}
        isExpandable={node.isPreviousQuery}
        onExpandClick={() => {
          if (!node.query) return;

          props.onExpandClick(node.query.groups, node.id);
        }}
        previousQueryLoading={node.loading}
        error={node.error}
      />
      {
        node.isPreviousQuery &&
        <p className="query-node__previous-query">
          {T.translate('queryEditor.previousQuery')}
        </p>
      }
      {
        node.error
        ? <ErrorMessage
            className="query-node__content"
            message={node.error}
          />
        : <p className="query-node__content">
            <span>{ node.label || node.id }</span>
            {
              node.description &&
              <span> - {node.description}</span>
            }
          </p>
      }
    </div>
  );
};

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props: PropsType): DraggedNodeType | DraggedQueryType {
    // Return the data describing the dragged item
    // NOT using `...node` since that would also spread `children` in.
    // This item may stem from either:
    // 1) A concept (dragged from CategoryTreeNode)
    // 2) A previous query (dragged from PreviousQueries)
    const { node, andIdx, orIdx } = props;

    const draggedNode = {
      moved: true,
      andIdx,
      orIdx,

      label: node.label,
      excludeTimestamps: node.excludeTimestamps,

      loading: node.loading,
      error: node.error,
    };

    if (node.isPreviousQuery)
      return {
        ...draggedNode,
        id: node.id,
        query: node.query,
        isPreviousQuery: true
      }
    else
      return {
        ...draggedNode,
        ids: node.ids,
        tree: node.tree,
        tables: node.tables,
      }
  }
};

/**
 * Specifies the dnd-related props to inject into the component.
 */
const collect = (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  isDragging: monitor.isDragging()
});

const DraggableQueryNode = DragSource(
  dndTypes.QUERY_NODE,
  nodeSource,
  collect
)(QueryNode);

export default AdditionalInfoHoverable(DraggableQueryNode);
