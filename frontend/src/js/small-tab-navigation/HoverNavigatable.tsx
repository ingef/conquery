import styled from "@emotion/styled";
import { ReactNode, useState } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { DNDType } from "../common/constants/dndTypes";
import { PossibleDroppableObject } from "../ui-components/Dropzone";

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

const Root = styled("div")<{
  isOver?: boolean;
  isDroppable?: boolean;
  highlightDroppable?: boolean;
}>`
  background-color: ${({ theme, isDroppable, highlightDroppable, isOver }) => {
    if (isOver && isDroppable)
      return highlightDroppable ? theme.col.grayLight : theme.col.grayVeryLight;
    if (highlightDroppable && isDroppable) return theme.col.grayVeryLight;
    return "inherit";
  }};
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
  display: inline-flex;
`;

// estimated to feel responsive, but not too quick
const TIME_UNTIL_NAVIGATE = 1000;

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

      if (timeoutVar == null) {
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
    <Root
      ref={drop}
      isOver={isOver}
      isDroppable={isDroppable}
      className={className}
      highlightDroppable={highlightDroppable}
    >
      {children}
    </Root>
  );
};
