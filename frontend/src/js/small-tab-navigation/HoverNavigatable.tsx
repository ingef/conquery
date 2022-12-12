import styled from "@emotion/styled";
import { ReactNode, useState } from "react";
import { useDrop } from "react-dnd";

import { DNDType } from "../common/constants/dndTypes";

interface PropsT {
  triggerNavigate: () => void;
  children: ReactNode;
  className?: string;
}

const Root = styled("div")<{
  isOver?: boolean;
}>`
  background-color: ${({ theme, isOver }) =>
    isOver ? `${theme.col.grayVeryLight}` : "inherit"};
  position: relative;
  display: inline-flex;
`;

// default time until the hover triggers navigation
// 600 ms is the time for the action to be triggered
// feels very responsive
const TIME_UNTIL_NAVIGATE = 600;

export const HoverNavigatable = ({ triggerNavigate, children, className }: PropsT) => {
  // Type Number is in Browser, however local typescript does not recognize it
  let [timeout, setTimeoutVariable] = useState<null | NodeJS.Timeout>(null);
  const [{ isOver }, drop] = useDrop({
    accept: [
      DNDType.FORM_CONFIG,
      DNDType.CONCEPT_TREE_NODE,
      DNDType.PREVIOUS_QUERY,
      DNDType.PREVIOUS_SECONDARY_ID_QUERY,
    ],
    hover: (_, monitor) => {
      if (timeout == null) {
        setTimeoutVariable(
          setTimeout(() => {
            setTimeoutVariable(null);
            if (monitor.isOver()) {
              triggerNavigate();
            }
          }, TIME_UNTIL_NAVIGATE),
        );
      }
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });
  return (
    <Root ref={drop} isOver={isOver} className={className}>
      {children}
    </Root>
  );
};
