import React                  from 'react';
import PropTypes              from 'prop-types';
import classnames             from 'classnames';
import T                      from 'i18n-react';
import { DropTarget }         from 'react-dnd';
import { NativeTypes }        from 'react-dnd-html5-backend';
import { dndTypes }           from '../common/constants';


const dropzoneTarget = {
  drop(props, monitor) {
    const item = monitor.getItem();

    if (item.files)
      props.onDropFiles(item);
    else
      props.onDropNode(item);

    if (item.isPreviousQuery)
      props.onLoadPreviousQuery(item.id);
  }
};

function collect(connect, monitor) {
  return {
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver()
  };
}

const QueryEditorDropzone = (props) => {
  return props.connectDropTarget(
    <div className={classnames(
      'query-editor-dropzone', {
        'query-editor-dropzone--initial': props.isInitial,
        'query-editor-dropzone--and': props.isAnd,
      }
    )}>
      <div className={classnames(
        'dropzone', {
          'dropzone--over': props.isOver,
        }
      )}>
        <p className="dropzone__text">
          {
            props.isInitial
            ? T.translate('dropzone.dragElementPlease')
            : T.translate('dropzone.dragElementPleaseShort')
          }
        </p>
      </div>
    </div>
  );
};

QueryEditorDropzone.propTypes = {
  isInitial: PropTypes.bool,
  isAnd: PropTypes.bool,
  connectDropTarget: PropTypes.func.isRequired,
  isOver: PropTypes.bool.isRequired,
  onDropNode: PropTypes.func.isRequired,
  onDropFiles: PropTypes.func.isRequired,
  onLoadPreviousQuery: PropTypes.func.isRequired,
};

export default DropTarget(
  [
    dndTypes.CATEGORY_TREE_NODE,
    dndTypes.QUERY_NODE,
    dndTypes.PREVIOUS_QUERY,
    NativeTypes.FILE
  ],
  dropzoneTarget,
  collect
)(QueryEditorDropzone);
