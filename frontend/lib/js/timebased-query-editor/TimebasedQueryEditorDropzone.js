// @flow

import React                  from 'react';
import { connect }            from 'react-redux';
import classnames             from 'classnames';
import T                      from 'i18n-react';
import { DropTarget }         from 'react-dnd';

import {
  PREVIOUS_QUERY,
  TIMEBASED_NODE,
} from '../common/constants/dndTypes';

import {
  removeTimebasedNode
} from './actions';


const dropzoneTarget = {
  drop(props, monitor) {
    const item = monitor.getItem();

    const { moved } = item;

    if (moved) {
      const { conditionIdx, resultIdx } = item;

      props.onRemoveTimebasedNode(conditionIdx, resultIdx, moved);
      props.onDropNode(item.node, moved);
    } else {
      props.onDropNode(item, false);
    }
  }
};

function collect(connect, monitor) {
  return {
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver()
  };
}

type PropsType = {
  isOver: boolean,
  onDropNode: () => void,
  connectDropTarget: () => void,
  onRemoveTimebasedNode: () => void,
};

const TimebasedQueryEditorDropzone = (props: PropsType) => {
  return props.connectDropTarget(
    <div className="timebased-query-editor-dropzone">
      <div className={classnames(
        'dropzone', {
          'dropzone--over': props.isOver,
        }
      )}>
        <p className="dropzone__text">
          {
            T.translate('dropzone.dragQuery')
          }
        </p>
      </div>
    </div>
  );
};

TimebasedQueryEditorDropzone.propTypes = {
};

const mapDispatchToProps = (dispatch) => ({
  onRemoveTimebasedNode: (conditionIdx, resultIdx, moved) =>
    dispatch(removeTimebasedNode(conditionIdx, resultIdx, moved)),
});

export default connect(
  () => ({}),
  mapDispatchToProps
)(DropTarget(
  [ PREVIOUS_QUERY, TIMEBASED_NODE ],
  dropzoneTarget,
  collect
)(TimebasedQueryEditorDropzone));
