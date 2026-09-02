import { type ReactNode, useState } from "react";
import { type DropTargetMonitor, useDrop } from "react-dnd";
import { tv } from "tailwind-variants";

import { DNDType } from "../common/constants/dndTypes";
import { exists } from "../common/helpers/exists";
import type { PossibleDroppableObject } from "../ui-components/Dropzone";

interface PropsT {
  triggerNavigate: () => void;
  children: ReactNode;
  className?: string;
  canDrop?: (
    item: PossibleDroppableObject,
    monitor: DropTargetMonitor<PossibleDroppableObject, unknown>,
  ) => boolean;
  highlightDroppable?: boolean;
}

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

// estimated to feel responsive, but not too quick
const TIME_UNTIL_NAVIGATE = 1300;

export const HoverNavigatable = ({
  triggerNavigate,
  children,
  className,
  canDrop,
  highlightDroppable,
}: PropsT) => {
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
    canDrop: canDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
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
