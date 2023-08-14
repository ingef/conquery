import styled from "@emotion/styled";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  lastElement?: boolean;
}

const RootHeightBase = 30;
const LineHeight = 3;
const MarginTopOffsetOver = 5;
const Root = styled("div")`
  width: 100%;
  left: 0;
  right: 0;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Line = styled("div")`
  overflow: hidden;
  display: block;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin: 1px 0;
  height: ${LineHeight}px;
  border-radius: 2px;
`;

const DropzoneBetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  onDrop,
  lastElement,
}: Props<DroppableObject>) => {
  const [{ isOver }, addZoneRef] = useDrop({
    accept: acceptedDropTypes,
    drop: onDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  const rootHeightMultiplier = lastElement ? 0.5 : 1;
  const rootMarginTop = (lastElement ? -15 : -5) - LineHeight;
  const rootDefaultTop = lastElement ? -5 : -10;
  const rootOverTop = (lastElement ? -5 : -15) - LineHeight;

  return (
    <>
      {isOver && <Line />}
      <Root
        ref={addZoneRef}
        style={{
          height:
            RootHeightBase * rootHeightMultiplier +
            (isOver ? 0 : LineHeight),
          marginTop: (isOver ? MarginTopOffsetOver : 0) + rootMarginTop,
          top: isOver ? rootOverTop : rootDefaultTop,
        }}
      ></Root>
    </>
  );
};

export default DropzoneBetweenElements;
