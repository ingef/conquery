import React from "react";
import classnames from "classnames";
import T from "i18n-react";

import { DropTarget } from "react-dnd";
import { dndTypes } from "../common/constants";

const dropzoneTarget = {
  drop(props, monitor) {
    const item = monitor.getItem();
    props.onDropConcept(item);
  },

  canDrop({ node }, monitor) {
    const item = monitor.getItem();
    // The dragged item should contain exactly one id
    // since it was dragged from the tree
    const conceptId = item.ids[0];
    return item.tree === node.tree && !node.ids.some(id => id === conceptId);
  }
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver(),
  canDrop: monitor.canDrop()
});

const ConceptDropzone = DropTarget(
  dndTypes.CATEGORY_TREE_NODE,
  dropzoneTarget,
  collect
)(props =>
  props.connectDropTarget(
    <div className="query-editor-dropzone">
      <div
        className={classnames("dropzone", {
          "dropzone--over": props.isOver && props.canDrop,
          "dropzone--disallowed": props.isOver && !props.canDrop
        })}
      >
        <p className="dropzone__text">
          {T.translate("queryNodeEditor.dropConcept")}
        </p>
      </div>
    </div>
  )
);

export default ConceptDropzone;
