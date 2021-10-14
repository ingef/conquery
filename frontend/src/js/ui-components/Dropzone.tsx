import styled from "@emotion/styled";
import React from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import type { DragItemFormConceptNode } from "../external-forms/form-concept-group/FormConceptNode";
import type { DragItemFormConfig } from "../external-forms/form-configs/FormConfig";
import type {
  DragItemConceptTreeNode,
  DragItemNode,
  DragItemQuery,
} from "../standard-query-editor/types";
import type { DragItemTimebasedNode } from "../timebased-query-editor/TimebasedNode";

import type { DragItemFile } from "./DropzoneWithFileInput";

const Root = styled("div")<{
  isOver?: boolean;
  naked?: boolean;
  canDrop?: boolean;
}>`
  border: ${({ theme, isOver, canDrop, naked }) =>
    naked
      ? "none"
      : isOver && !canDrop
      ? `3px solid ${theme.col.red}`
      : isOver
      ? `3px solid ${theme.col.black}`
      : `3px dashed ${theme.col.gray}`};
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: ${({ naked }) => (naked ? "0" : "10px")};
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${({ theme, canDrop, naked, isOver }) =>
    naked && isOver && canDrop
      ? theme.col.grayLight
      : canDrop
      ? theme.col.grayVeryLight
      : theme.col.bg};
  width: 100%;
  color: ${({ theme, isOver, canDrop }) =>
    isOver && !canDrop
      ? theme.col.red
      : isOver
      ? theme.col.black
      : theme.col.gray};
`;

export interface ChildArgs {
  isOver: boolean;
  canDrop: boolean;
  itemType: string | symbol | null;
}

export interface DropzoneProps<DroppableObject> {
  className?: string;
  acceptedDropTypes: string[];
  naked?: boolean;
  onDrop: (props: DroppableObject, monitor: DropTargetMonitor) => void;
  canDrop?: (props: DroppableObject, monitor: DropTargetMonitor) => boolean;
  onClick?: () => void;
  children?: (args: ChildArgs) => React.ReactNode;
}

export type PossibleDroppableObject =
  | DragItemFile
  | DragItemNode
  | DragItemTimebasedNode
  | DragItemQuery
  | DragItemConceptTreeNode
  | DragItemFormConfig
  | DragItemFormConceptNode;

const Dropzone = <DroppableObject extends PossibleDroppableObject>({
  className,
  acceptedDropTypes,
  naked,
  canDrop,
  onDrop,
  onClick,
  children,
}: DropzoneProps<DroppableObject>) => {
  const [{ canDrop: canDropResult, isOver, itemType }, dropRef] = useDrop<
    DroppableObject,
    void,
    ChildArgs
  >({
    accept: acceptedDropTypes,
    drop: onDrop,
    canDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
      itemType: monitor.getItemType(),
    }),
  });

  return (
    <Root
      ref={dropRef}
      isOver={isOver}
      canDrop={canDropResult}
      className={className}
      onClick={onClick}
      naked={naked}
    >
      {children && children({ isOver, canDrop: canDropResult, itemType })}
    </Root>
  );
};

export default Dropzone;
