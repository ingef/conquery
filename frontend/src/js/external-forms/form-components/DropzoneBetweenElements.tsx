import styled from "@emotion/styled";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { PossibleDroppableObject } from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  isFirstElement: boolean;
}

const Root = styled("div")<{
  isOver: boolean;
  isDroppable: boolean;
  isFirstElement: boolean;
}>`
  width: 100%;
  left: 0;
  top: -17px;
  height: 40px;
  right: 0;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const DropzoneContainer = styled("div")`
  overflow: hidden;
  height: 20px;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  onDrop,
  isFirstElement,
}: Props<DroppableObject>) => {
  const [{ isOver, isDroppable }, addZoneRef] = useDrop({
    accept: acceptedDropTypes,
    drop: onDrop,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });
  const [{ isOver: isOver2 }, dropzoneWrapperRef] = useDrop({
    accept: acceptedDropTypes,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });
  console.log(isOver, isDroppable);

  return (
    <>
      <Root
        ref={addZoneRef}
        isOver={isOver}
        isDroppable={isDroppable}
        isFirstElement={isFirstElement}
      ></Root>

      {(isOver || isOver2) && (
        <DropzoneContainer ref={dropzoneWrapperRef}></DropzoneContainer>
      )}
    </>
  );
};

export default BetweenElements;
