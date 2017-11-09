// @flow

import React                       from 'react';
import T                           from 'i18n-react';
import { DragSource }              from 'react-dnd';

import { dndTypes }                from '../common/constants';
import { AdditionalInfoHoverable } from '../tooltip';
import { ErrorMessage }            from '../error-message';

import QueryNodeActions            from './QueryNodeActions';

import type { ElementType }        from './types';

type PropsType =  {
  node: ElementType,
  onDeleteNode: Function,
  onFilterClick: Function,
  onExpandClick: Function,
  connectDragSource: Function,
  andIdx: number,
  orIdx: number,
};

const QueryNode = (props: PropsType) => {
  const { node } = props;

  return props.connectDragSource(
    <div className="query-node">
      <QueryNodeActions
        hasActiveFilters={node.hasActiveFilters}
        onFilterClick={props.onFilterClick}
        onDeleteNode={props.onDeleteNode}
        isExpandable={!!node.query}
        onExpandClick={() => {
          if (!node.query) return;

          props.onExpandClick(node.query.groups);
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
  beginDrag(props) {
    // Return the data describing the dragged item
    // NOT using `...node` since that would also spread `children` in.
    // This item may stem from either:
    // 1) A concept (dragged from CategoryTreeNode)
    // 2) A previous query (dragged from PreviouQueries)
    const { node, andIdx, orIdx } = props;

    return {
      andIdx,
      orIdx,
      id: node.id,
      label: node.label,
      description: node.description,
      tables: node.tables,
      additionalInfos: node.additionalInfos,
      matchingEntries: node.matchingEntries,
      hasActiveFilters: node.hasActiveFilters,
      excludeTimestamps: node.excludeTimestamps,
      isPreviousQuery: node.isPreviousQuery,
      loading: node.loading,
      error: node.error,
      query: node.query,
      moved: true,
    };
  }
};

/**
 * Specifies the dnd-related props to inject into the component.
 */
function collect(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging()
  };
}


const DraggableQueryNode = DragSource(
  dndTypes.QUERY_NODE,
  nodeSource,
  collect
)(QueryNode);

export default AdditionalInfoHoverable(DraggableQueryNode);
