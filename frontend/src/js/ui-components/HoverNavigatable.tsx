import { type ReactNode, useState } from "react";
import { type DropTargetMonitor, useDrop } from "react-dnd";
import { tv } from "tailwind-variants";

import { DNDType } from "../common/constants/dndTypes";
import { exists } from "../common/helpers/exists";
import type { PossibleDroppableObject } from "./Dropzone";

type CanDrop = (
  item: PossibleDroppableObject,
  monitor: DropTargetMonitor<PossibleDroppableObject, unknown>,
) => boolean;

// estimated to feel responsive, but not too quick
const TIME_UNTIL_NAVIGATE = 1300;

/**
 * Makes an element a drop target that navigates somewhere when a dragged item
 * hovers over it for a moment, like a tab that switches while dragging a
 * concept onto it. Connect `drop` to the element.
 */
export const useHoverNavigate = ({
  triggerNavigate,
  canDrop,
}: {
  triggerNavigate: () => void;
  canDrop?: CanDrop;
}) => {
  const [timeoutVar, setTimeoutVar] = useState<null | NodeJS.Timeout>(null);

  const [{ isOver, isDroppable }, drop] = useDrop({
    accept: [
      DNDType.FORM_CONFIG,
      DNDType.CONCEPT_TREE_NODE,
      DNDType.PREVIOUS_QUERY,
      DNDType.PREVIOUS_SECONDARY_ID_QUERY,
    ],
    hover: (_, monitor) => {
      if (!isDroppable) return;

      if (!exists(timeoutVar)) {
        setTimeoutVar(
          setTimeout(() => {
            setTimeoutVar(null);
            if (monitor.isOver()) {
              triggerNavigate();
            }
          }, TIME_UNTIL_NAVIGATE),
        );
      }
    },
    canDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  return { drop, isOver, isDroppable };
};

const root = tv({
  base: ["relative", "inline-flex", "rounded", "bg-inherit"],
  variants: {
    isOver: { true: "", false: "" },
    isDroppable: { true: "", false: "" },
    highlightDroppable: { true: "", false: "" },
  },
  // later wins when several match
  compoundVariants: [
    { isDroppable: true, highlightDroppable: true, class: "bg-gray-50" },
    { isOver: true, isDroppable: true, class: "bg-gray-50" },
    {
      isOver: true,
      isDroppable: true,
      highlightDroppable: true,
      class: "bg-gray-100",
    },
  ],
});

export const HoverNavigatable = ({
  triggerNavigate,
  children,
  className,
  canDrop,
  highlightDroppable,
}: {
  triggerNavigate: () => void;
  children: ReactNode;
  className?: string;
  canDrop?: CanDrop;
  highlightDroppable?: boolean;
}) => {
  const { drop, isOver, isDroppable } = useHoverNavigate({
    triggerNavigate,
    canDrop,
  });

  return (
    <div
      ref={(el) => {
        drop(el);
      }}
      className={root({
        isOver,
        isDroppable,
        highlightDroppable: !!highlightDroppable,
        className,
      })}
    >
      {children}
    </div>
  );
};
