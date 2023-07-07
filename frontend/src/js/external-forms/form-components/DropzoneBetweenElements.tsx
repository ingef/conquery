import styled from "@emotion/styled";
import { useState } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
}

const RootHeightBase = 40;

const Root = styled("div")`
  width: 100%;
  left: 0;
  top: -15px;
  right: 0;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Expander = styled("div")`
  overflow: hidden;
  display: block;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  onDrop,
}: Props<DroppableObject>) => {
  const [height, setHeight] = useState(0);

  const [{ isOver }, addZoneRef] = useDrop({
    accept: acceptedDropTypes,
    drop: onDrop,
    hover(item) {
      if (item.type === "CONCEPT_TREE_NODE") {
        return setHeight(item.dragContext.height);
      }
    },

    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  return (
    <>
      <Root ref={addZoneRef} style={{height: RootHeightBase + (isOver ? height : 0)}}></Root>
      {isOver && <Expander style={{height: height}}/>}
    </>
  );
};

export default BetweenElements;
