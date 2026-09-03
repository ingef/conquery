import type { ReactNode, Ref } from "react";
import { type DropTargetMonitor, useDrop } from "react-dnd";
import { tv } from "tailwind-variants";

import { DNDType } from "../common/constants/dndTypes";
import { exists } from "../common/helpers/exists";
import type { DragItemFormConfig } from "../external-forms/types";
import type {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";

import type { DragItemFile } from "./DropzoneWithFileInput";

const root = tv({
  base: [
    "flex items-center justify-center",
    "w-full",
    "p-[10px]",
    "rounded",
    "border-[3px] border-dashed border-gray-400",
    "bg-bg-50",
    "text-gray-500",
  ],
  variants: {
    bare: { true: "p-0" },
    invisible: { true: "hidden" },
    // later wins when several are set
    transparent: { true: "bg-transparent" },
    canDrop: { true: "bg-gray-50" },
    isOver: { true: "border-solid border-gray-800 text-gray-800" },
    naked: { true: "border-none" },
  },
  compoundVariants: [
    { isOver: true, canDrop: false, class: "border-red text-red" },
    { naked: true, isOver: true, canDrop: true, class: "bg-gray-100" },
  ],
});

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

const Dropzone = <DroppableObject extends PossibleDroppableObject>({
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
  ref,
}: DropzoneProps<DroppableObject> & { ref?: Ref<HTMLDivElement> }) => {
  /*  actually, not "any", but ChildArgs<DroppableObject>. But I can't get that to work in JSX */
  const [{ canDrop: canDropResult, isOver, item }, dropRef] = useDrop<
    DroppableObject,
    void,
    {
      canDrop: boolean;
      isOver: boolean;
      item: unknown;
    }
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
    // biome-ignore lint/a11y/noStaticElementInteractions: drop target, click opens a file dialog
    // biome-ignore lint/a11y/useKeyWithClickEvents: drop target, click opens a file dialog
    <div
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
      className={root({
        isOver,
        invisible: !!invisible && !canDropResult,
        canDrop: canDropResult,
        naked,
        transparent,
        bare,
        className,
      })}
      onClick={onClick}
    >
      {children?.({
        isOver,
        canDrop: canDropResult,
        item: item as DroppableObject, // Casting because see comment above
      })}
    </div>
  );
};

export default Dropzone;
