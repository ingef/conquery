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

const DropzoneBetweenElements = <
  DroppableObject extends PossibleDroppableObject,
>({
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
  const rootDefaultMarginTop = (lastElement ? -15 : -5) - LineHeight;
  const rootOverMarginTop = lastElement ? -23 : -10;
  const rootDefaultTop = lastElement ? -5 : -10;
  const rootOverTop = (lastElement ? -2 : -15) - LineHeight;

  return (
    <>
      {isOver && <Line />}
      <Root
        ref={addZoneRef}
        style={{
          height:
            RootHeightBase * rootHeightMultiplier +
            (isOver && !lastElement ? 0 : LineHeight) +
            (lastElement ? LineHeight + 4 : 0),
          marginTop: isOver ? rootOverMarginTop : rootDefaultMarginTop,
          top: isOver ? rootOverTop : rootDefaultTop,
        }}
      ></Root>
    </>
  );
};

export default DropzoneBetweenElements;
