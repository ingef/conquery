import React from "react";
import { findDOMNode } from "react-dom";

import { DragSource } from "react-dnd";

import * as dndTypes from "../../common/constants/dndTypes";
import { DatasetIdT } from "../../api/types";

import type { DraggedQueryType } from "../../standard-query-editor/types";
import { PreviousQueryT } from "./reducer";
import PreviousQuery from "./PreviousQuery";

const nodeSource = {
  beginDrag(props, monitor, component): DraggedQueryType {
    const { width, height } = findDOMNode(component).getBoundingClientRect();
    // Return the data describing the dragged item
    return {
      width,
      height,
      id: props.query.id,
      label: props.query.label,
      isPreviousQuery: true,
    };
  },
};

// These props get injected into the component
function collect(connect, monitor) {
  return {
    connectDragSource: connect.dragSource(),
    isDragging: monitor.isDragging(),
  };
}

type PropsType = {
  query: PreviousQueryT;
  datasetId: DatasetIdT;
  connectDragSource: () => void;
};

// Has to be a class because of https://github.com/react-dnd/react-dnd/issues/530
class PreviousQueryDragContainer extends React.Component {
  props: PropsType;

  render() {
    const { query, datasetId, connectDragSource } = this.props;

    const isNotEditing = !(query.editingLabel || query.editingTags);

    return (
      <PreviousQuery
        ref={(instance) => {
          if (isNotEditing) connectDragSource(instance);
        }}
        query={query}
        datasetId={datasetId}
      />
    );
  }
}

export default DragSource(
  dndTypes.PREVIOUS_QUERY,
  nodeSource,
  collect
)(PreviousQueryDragContainer);
