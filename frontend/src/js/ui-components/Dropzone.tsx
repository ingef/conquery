import styled from "@emotion/styled";
import { ForwardedRef, forwardRef, ReactNode, useEffect } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { DNDType } from "../common/constants/dndTypes";
import { exists } from "../common/helpers/exists";
import { DragItemFormConfig } from "../external-forms/types";
import type {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";

import type { DragItemFile } from "./DropzoneWithFileInput";

const Root = styled("div")<{
  isOver?: boolean;
  naked?: boolean;
  bare?: boolean;
  transparent?: boolean;
  canDrop?: boolean;
  invisible?: boolean;
}>`
  border: ${({ theme, isOver, canDrop, naked }) =>
    naked
      ? "none"
      : isOver && !canDrop
      ? `3px solid ${theme.col.red}`
      : isOver
      ? `3px solid ${theme.col.black}`
      : `3px dashed ${theme.col.grayMediumLight}`};
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: ${({ bare }) => (bare ? "0" : "10px")};
  display: ${({ invisible }) => (invisible ? "none" : "flex")};
  align-items: center;
  justify-content: center;
  background-color: ${({ theme, canDrop, naked, isOver, transparent }) =>
    naked && isOver && canDrop
      ? theme.col.grayLight
      : canDrop
      ? theme.col.grayVeryLight
      : transparent
      ? "transparent"
      : theme.col.bg};
  width: 100%;
  color: ${({ theme, isOver, canDrop }) =>
    isOver && !canDrop
      ? theme.col.red
      : isOver
      ? theme.col.black
      : theme.col.gray};
`;

export interface ChildArgs<DroppableObject> {
  isOver: boolean;
  canDrop: boolean;
  item: DroppableObject;
}

export interface DropzoneProps<DroppableObject> {
  className?: string;
  acceptedDropTypes: string[];
  naked?: boolean;
  bare?: boolean;
  transparent?: boolean;
  invisible?: boolean;
  onDrop: (props: DroppableObject, monitor: DropTargetMonitor) => void;
  canDrop?: (props: DroppableObject, monitor: DropTargetMonitor) => boolean;
  onClick?: () => void;
  children?: (args: ChildArgs<DroppableObject>) => ReactNode;
}

export type PossibleDroppableObject =
  | DragItemFile
  | DragItemQuery
  | DragItemConceptTreeNode
  | DragItemFormConfig;

export const isMovedObject = (
  item: PossibleDroppableObject,
): item is PossibleDroppableObject & {
  dragContext: { movedFromAndIdx: number; movedFromOrIdx: number };
} => {
  switch (item.type) {
    case "__NATIVE_FILE__":
      return false;
    case DNDType.FORM_CONFIG:
      return false;
    case DNDType.CONCEPT_TREE_NODE:
    case DNDType.PREVIOUS_QUERY:
    case DNDType.PREVIOUS_SECONDARY_ID_QUERY:
      return (
        exists(item.dragContext.movedFromAndIdx) &&
        exists(item.dragContext.movedFromOrIdx)
      );
  }
};

const Dropzone = <DroppableObject extends PossibleDroppableObject>(
  {
    className,
    acceptedDropTypes,
    naked,
    transparent,
    bare,
    canDrop,
    onDrop,
    onClick,
    invisible,
    children,
  }: DropzoneProps<DroppableObject>,
  ref?: ForwardedRef<HTMLDivElement>,
) => {
  /*  actually, not "any", but ChildArgs<DroppableObject>. But I can't get that to work in JSX */
  const [{ canDrop: canDropResult, isOver, item }, dropRef] = useDrop<
    DroppableObject,
    void,
    any
  >({
    accept: acceptedDropTypes,
    drop: onDrop,
    canDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
      item: monitor.getItem(),
    }),
  });

  return (
    <Root
      ref={(instance) => {
        dropRef(instance);

        // TODO: Probably a way to improve this, maybe find a good mergeRef helper
        if (ref) {
          if (typeof ref === "object") {
            ref.current = instance;
          } else {
            ref(instance);
          }
        }
      }}
      isOver={isOver}
      invisible={invisible && !canDropResult}
      canDrop={canDropResult}
      className={className}
      onClick={onClick}
      naked={naked}
      transparent={transparent}
      bare={bare}
    >
      {children &&
        children({
          isOver,
          canDrop: canDropResult,
          item: item as DroppableObject, // Casting because see comment above
        })}
    </Root>
  );
};

export default forwardRef(Dropzone) as <
  DroppableObject extends PossibleDroppableObject,
>(
  props: DropzoneProps<DroppableObject> & {
    ref?: ForwardedRef<HTMLDivElement>;
  },
) => JSX.Element;
