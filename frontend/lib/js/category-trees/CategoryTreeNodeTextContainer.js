// @flow

import React                        from 'react'
import { DragSource }               from 'react-dnd';
import Highlighter                  from 'react-highlight-words';
import classnames                   from 'classnames';

import { AdditionalInfoHoverable }  from '../tooltip';
import { isEmpty }                  from '../common/helpers';
import { dndTypes }                 from '../common/constants';
import { type SearchType }          from './reducer';

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
  search?: SearchType,
};

const CategoryTreeNodeTextContainer = (props: PropsType) => {
  const zeroEntries = !isEmpty(props.node.matchingEntries) && props.node.matchingEntries === 0;
  const searching = props.search && props.search.words.length > 0;

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
              'fa-folder-open': !!props.open || searching,
              'fa-folder': !props.open || !searching
            }
          )} />
        }
        <span>
          {
            searching
            ? (<Highlighter
                searchWords={props.search.words}
                autoEscape={true}
                textToHighlight={props.node.label}
              />)
            : (props.node.label)
          }
        </span>
        {
          searching && props.node.description
          ? (<Highlighter
              searchWords={props.search.words}
              autoEscape={true}
              textToHighlight={props.node.description}
            />)
          : (props.node.description)
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
    return props.node;
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
