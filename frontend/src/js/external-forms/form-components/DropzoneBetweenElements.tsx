import styled from "@emotion/styled";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  lastElement?: boolean;
}

const RootHeightBase = 40;
const lineHeight = 3;

const Root = styled("div")<{ lastElement?: boolean }>`
  width: 100%;
  left: 0;
  right: 0;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Line = styled("div")`
  overflow: hidden;
  display: block;
  background-color: ${({ theme }) => theme.col.blueGrayLight};
  margin: 4px 0;
  height: ${lineHeight}px;
  border-radius: 2px;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
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
  const rootDefaultMarginTop = (lastElement ? -15 : -5) - lineHeight;
  const rootOverMarginTop = lastElement ? -29 : -12;
  const rootDefaultTop = lastElement ? -5 : -15;
  const rootOverTop = (lastElement ? -2 : -19) - lineHeight;

  return (
    <>
      {isOver && <Line />}
      <Root
        ref={addZoneRef}
        style={{
          height:
            RootHeightBase * rootHeightMultiplier +
            (isOver && !lastElement ? 0 : lineHeight + 4) +
            (lastElement ? lineHeight + 4 : 0),
          marginTop: isOver ? rootOverMarginTop : rootDefaultMarginTop,
          top: isOver ? rootOverTop : rootDefaultTop,
        }}
        lastElement={lastElement}
      ></Root>
    </>
  );
};

export default BetweenElements;
