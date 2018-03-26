// @flow

import React                       from 'react'
import { DragSource }              from 'react-dnd';
import classnames                  from 'classnames';

import { AdditionalInfoHoverable } from '../tooltip';
import { isEmpty }                 from '../common/helpers';
import { dndTypes }                from '../common/constants';

type PropsType = {
  node: {
    id: number | string,
    label: string,
    hasChildren: boolean,
    description?: string,
    tables?: [],
    matchingEntries?: number,
    additionalInfos?: [],
  },
  open: boolean,
  depth: number,
  active?: boolean,
  onTextClick?: Function,
  connectDragSource: Function,
};

const CategoryTreeNodeTextContainer = (props: PropsType) => {
  const zeroEntries = !isEmpty(props.node.matchingEntries) && props.node.matchingEntries === 0;

  const render = (
    <div
      className="category-tree-node__text-container"
      onClick={props.onTextClick}
      style={{paddingLeft: props.depth * 20}}
    >
      <p
        className={classnames(
          "category-tree-node__text", {
            "category-tree-node__text--open" : !!props.open,
            "category-tree-node__text--zero" : zeroEntries,
          }
        )}
      >
        {
          props.node.hasChildren &&
          <i className={classnames(
            'category-tree-node__icon',
            'fa', {
              'fa-folder-open': !!props.open,
              'fa-folder': !props.open
            }
          )} />
        }
        <span>
          { props.node.label }
        </span>
        {
          props.node.description &&
          <span> - { props.node.description }</span>
        }
      </p>
    </div>
  );

  // Don't allow dragging with inactive elements
  return props.active === false
    ? render
    : props.connectDragSource(render);
};


const HoverableCategoryTreeNodeTextContainer = AdditionalInfoHoverable(
  CategoryTreeNodeTextContainer
);

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props) {
    // Return the data describing the dragged item
    // by creating a concept list
    return {
      ids: [props.node.id],
      label: props.node.label,
      tables: props.node.tables
    };
  }
};

/**
 * Specifies the props to inject into your component.
 */
function collect(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging()
  };
}

export default DragSource(
  dndTypes.CATEGORY_TREE_NODE,
  nodeSource,
  collect
)(HoverableCategoryTreeNodeTextContainer);
