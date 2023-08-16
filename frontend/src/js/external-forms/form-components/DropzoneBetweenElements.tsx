import styled from "@emotion/styled";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  lastElement?: boolean;
  top?: number;
}

const RootHeightBase = 30;
const LineHeight = 3;
const Root = styled("div")`
  width: 100%;
  left: 0;
  right: 0;
  position: absolute;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Line = styled("div")<{show:boolean}>`
  overflow: hidden;
  display: block;
  visibility: ${({show}) => show ? "visible" : "hidden"};
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
  top
}: Props<DroppableObject>) => {
  const [{ isOver }, addZoneRef] = useDrop({
    accept: acceptedDropTypes,
    drop: onDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  const rootHeightMultiplier = lastElement ? 0.7 : 1;

  return (
    <>
      <Line show={isOver}/>
      <Root
        ref={addZoneRef}
        style={{
          height:
            RootHeightBase * rootHeightMultiplier,
          top: top,
        }}
      ></Root>
    </>
  );
};

export default DropzoneBetweenElements;
