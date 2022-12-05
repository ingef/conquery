import styled from "@emotion/styled";
import { ReactNode, useState } from "react";
import { useDrop } from "react-dnd";

import { DNDType } from "../common/constants/dndTypes";

interface PropsT {
  triggerNavigate: () => void;
  children: ReactNode;
}

const Root = styled("div")<{
  isOver?: boolean;
}>`
  border: ${({ theme, isOver }) =>
    isOver ? `1px solid ${theme.col.grayMediumLight}` : "none"};
  position: relative;
  display: inline-flex;
  color: ${({ theme, isOver }) =>
    isOver ? theme.col.red : isOver ? theme.col.black : theme.col.gray};
`;

// default time until the hover triggers navigation
// 600 ms is the default time for a tooltip to appear
// feels very responsive
// can be overriden by passing a timeUntilNavigateOverride prop
const TIME_UNTIL_NAVIGATE = 600;

export const HoverNavigatable = ({ triggerNavigate, children }: PropsT) => {
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
    <Root ref={drop} isOver={isOver}>
      {children}
    </Root>
  );
};

export const FlexHoverNavigatable = styled(HoverNavigatable)`
  flex: flex;
  display: flex;
`;
