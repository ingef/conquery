import styled from "@emotion/styled";
import { ReactNode, useState } from "react";
import { useDrop } from "react-dnd";
import { DNDType } from "../common/constants/dndTypes";


interface PropsT {
  triggerNavigate: () => void;
  children: ReactNode;
}

const Root = styled("div")`
  position: relative;
  display: inline-flex;
`;

// default time until the hover triggers navigation
// 600 ms is the default time for a tooltip to appear
// feels very responsive
// can be overriden by passing a timeUntilNavigateOverride prop
const TIME_UNTIL_NAVIGATE = 600;

export const HoverNavigatable = ({ triggerNavigate, children }: PropsT) => {
  // Type Number is in Browser, however local typescript does not recognize it
  let [timeout, setTimeoutVariable] = useState<Number | null | NodeJS.Timeout>(null);
  const [_, drop] = useDrop({
    accept: [DNDType.FORM_CONFIG, DNDType.CONCEPT_TREE_NODE,
    DNDType.PREVIOUS_QUERY, DNDType.PREVIOUS_SECONDARY_ID_QUERY],
    hover: (_, monitor) => {
      if (timeout === null) {
        setTimeoutVariable(setTimeout(() => {
          timeout = null;
          if (monitor.isOver()) {
            triggerNavigate();
          }

        }, TIME_UNTIL_NAVIGATE));
      }
    },
  });
  return (
    <Root ref={drop}>
      {children}
    </Root>
  )
};

export const FlexHoverNavigatable = styled(HoverNavigatable)`
  background-color: coral;
  flex: flex;
  display: flex;

`;
