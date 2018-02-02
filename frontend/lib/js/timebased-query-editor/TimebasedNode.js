// @flow

import React                    from 'react';
import T                        from 'i18n-react';
import classnames               from 'classnames';
import { DragSource }           from 'react-dnd';

import { VerticalToggleButton } from '../form-components';
import {
  FIRST,
  LAST,
  RANDOM
}                               from '../common/constants/timebasedQueryTimestampTypes';
import { TIMEBASED_NODE }       from '../common/constants/dndTypes';

type PropsType = {
  node: Object,
  position: 'left' | 'right',
  isIndexResult: boolean,
  onRemove: Function,
  onSetTimebasedNodeTimestamp: Function,
  onSetTimebasedIndexResult: Function,
  conditionIdx: number,
  resultIdx: number,
  connectDragSource: Function,
  isIndexResultDisabled: boolean,
};

const TimebasedNode = (props: PropsType) => {
  const toggleButton = (
    <VerticalToggleButton
      onToggle={props.onSetTimebasedNodeTimestamp}
      activeValue={props.node.timestamp}
      options={[
        { label: T.translate('timebasedQueryEditor.timestampFirst'), value: FIRST },
        { label: T.translate('timebasedQueryEditor.timestampRandom'), value: RANDOM },
        { label: T.translate('timebasedQueryEditor.timestampLast'), value: LAST },
      ]}
    />
  );

  return props.connectDragSource(
    <div className={classnames("timebased-node", `timebased-node--${props.position}`)}>
      <div className="timebased-node__container">
        <div className="timebased-node__content">
          <div className="timebased-node__timestamp">
            <p className="timebased-node__timestamp__title">
              { T.translate('timebasedQueryEditor.timestamp') }
            </p>
            { toggleButton }
          </div>
          <div className="timebased-node__description">
            <span
              onClick={props.onRemove}
              className="timebased-condition__remove-btn btn btn--small btn--icon"
            >
              <i className="fa fa-close" />
            </span>
            <p className="timebased-node__description__text">
              { props.node.label || props.node.id }
            </p>
          </div>
        </div>
        <button
          className={classnames(
            "timebased-node__index-result-btn", {
              "timebased-node__index-result-btn--active": props.isIndexResult,
              "timebased-node__index-result-btn--disabled": props.isIndexResultDisabled,
            },
          )}
          disabled={props.isIndexResultDisabled}
          onClick={props.onSetTimebasedIndexResult}
        >
          { T.translate('timebasedQueryEditor.timestampResultsFrom') }
        </button>
      </div>
    </div>
  );
};

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props) {
    // Return the data describing the dragged item
    const { node, conditionIdx, resultIdx } = props;

    return {
      conditionIdx,
      resultIdx,
      node,
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


const DraggableTimebasedNode = DragSource(
  TIMEBASED_NODE,
  nodeSource,
  collect
)(TimebasedNode);

export default DraggableTimebasedNode;
