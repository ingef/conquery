// @flow

import React from "react";
import classnames from "classnames";
import T from "i18n-react";
import { DropTarget, type ConnectDropTarget } from "react-dnd";
import { NativeTypes } from "react-dnd-html5-backend";

import { dndTypes } from "../common/constants";
import type { QueryIdType } from "../common/types/backend";
import type { DraggedNodeType, DraggedQueryType } from "./types";

type DraggedFileType = Object;

type PropsType = {
  isInitial: ?boolean,
  isAnd: ?boolean,
  onDropNode: (DraggedNodeType | DraggedQueryType) => void,
  onDropFile: DraggedFileType => void,
  onLoadPreviousQuery: QueryIdType => void,

  connectDropTarget: ConnectDropTarget,
  isOver: boolean
};

const dropzoneTarget = {
  drop(props, monitor) {
    const item = monitor.getItem();

    if (item.files) {
      props.onDropFile(item.files[0]);
    } else {
      props.onDropNode(item);

      if (item.isPreviousQuery) props.onLoadPreviousQuery(item.id);
    }
  }
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver()
});

// When instantiating the QueryEditorDropzone, flow doesn't recognize that
// connectDropTarget and isOver are being injected by react-dnd :(
const InnerQueryEditorDropzone = ({
  isOver,
  isAnd,
  isInitial,
  connectDropTarget
}: PropsType) => {
  return (
    <div
      ref={instance => connectDropTarget(instance)}
      className={classnames("query-editor-dropzone", {
        "query-editor-dropzone--initial": isInitial,
        "query-editor-dropzone--and": isAnd
      })}
    >
      <div
        className={classnames("dropzone", {
          "dropzone--over": isOver
        })}
      >
        <p className="dropzone__text">
          {isInitial
            ? T.translate("dropzone.dragElementPlease")
            : T.translate("dropzone.dragElementPleaseShort")}
        </p>
      </div>
    </div>
  );
};

export const QueryEditorDropzone = DropTarget(
  [
    dndTypes.CATEGORY_TREE_NODE,
    dndTypes.QUERY_NODE,
    dndTypes.PREVIOUS_QUERY,
    NativeTypes.FILE
  ],
  dropzoneTarget,
  collect
)(InnerQueryEditorDropzone);
