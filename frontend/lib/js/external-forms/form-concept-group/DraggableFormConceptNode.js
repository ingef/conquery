import React from "react";
import { DragSource } from "react-dnd";
import { dndTypes } from "../../common/constants";

type PropsT = {
  connectDragSource: () => void
};

const DraggableFormConceptNode = (props: PropsT) => {
  return (
    <div
      ref={instance => {
        props.connectDragSource(instance);
      }}
    >
      {props.children}
    </div>
  );
};

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props: PropsType, monitor, component): DraggedNodeType {
    const { width, height } = findDOMNode(component).getBoundingClientRect();

    return {
      width,
      height,
      ...props.createQueryElement()
    };
  }
};

/**
 * Specifies the props to inject into your component.
 */
const collect = (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  isDragging: monitor.isDragging()
});

export default DragSource(dndTypes.FORM_CONCEPT_NODE, nodeSource, collect)(
  DraggableFormConceptNode
);
