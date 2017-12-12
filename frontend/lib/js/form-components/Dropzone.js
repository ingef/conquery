import React          from 'react';
import { DropTarget } from 'react-dnd';
import classnames     from 'classnames';

// Decorates a Component with a Dropzone
// Then, the component is replaced with a Dropzone until "containsItem" is true
const Dropzone = (Component, acceptedDropTypes, onDrop) => {
  const dropzoneTarget = { drop: onDrop };

  const collect = (connect, monitor) => ({
    connectDropTarget: connect.dropTarget(),
    isOver: monitor.isOver()
  });

  type PropsType = {
    className?: string,
    connectDropTarget: Function,
    isOver: boolean,
    dropzoneText: string,
    containsItem: boolean,
  };

  class Zone extends React.Component {
    props: PropsType

    render() {
      const {
        className,
        isOver,
        connectDropTarget,
        dropzoneText,
        containsItem,
      } = this.props;

      if (containsItem) return Component;

      return connectDropTarget(
        <div className={classnames(
          'dropzone', {
            'dropzone--over': isOver,
          },
          className
        )}>
          <p className="dropzone__text">
            { dropzoneText }
          </p>
        </div>
      );
    }
  }

  return DropTarget(
    acceptedDropTypes,
    dropzoneTarget,
    collect
  )(Zone);
};

export default Dropzone;
