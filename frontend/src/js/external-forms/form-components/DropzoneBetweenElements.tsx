import styled from "@emotion/styled";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  lastElement?: boolean;
}

const RootHeightBase = 40;

const Root = styled("div")<{ lastElement?: boolean }>`
  width: 100%;
  left: 0;
  top: ${({ lastElement }) => (lastElement ? -10 : -15)}px;
  right: 0;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Line = styled("div")`
  overflow: hidden;
  display: block;
  background-color: ${({ theme }) => theme.col.blueGrayLight};
  margin: 5px 0;
  height: 2px;
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
  const rootDefaultMarginTop = lastElement ? -10 : -5;
  const rootOverMarginTop = lastElement ? -22 : -12;

  return (
    <>
      {isOver && <Line />}
      <Root
        ref={addZoneRef}
        style={{
          height: RootHeightBase * rootHeightMultiplier,
          marginTop: isOver ? rootOverMarginTop : rootDefaultMarginTop,
        }}
        lastElement={lastElement}
      ></Root>
    </>
  );
};

export default BetweenElements;
