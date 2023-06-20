import styled from "@emotion/styled";
import { useState } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
}

const Root = styled("div")<{
  height: number;
}>`
  width: 100%;
  left: 0;
  top: -15px;
  height: ${({ height }) => height + 40}px;
  right: 0;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const DropzoneContainer = styled("div")<{
  height: number;
}>`
  overflow: hidden;
  margin-top: ${({ height }) => -height}px;
  display: block;
  height: ${({ height }) => height}px;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  onDrop,
}: Props<DroppableObject>) => {
  const [height, setHeight] = useState(40);

  const [{ isOver }, addZoneRef] = useDrop({
    accept: acceptedDropTypes,
    drop: onDrop,
    hover(item) {
      if (item.type === "CONCEPT_TREE_NODE") {
        return setHeight(item.dragContext.height);
      }
      return setHeight(0);
    },

    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  return (
    <>
      <Root
        ref={addZoneRef}
        height={isOver  ? height : 0}
      ></Root>
      {isOver && (
        <DropzoneContainer height={height}/>
      )}
    </>
  );
};

export default BetweenElements;
