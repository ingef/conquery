import styled from "@emotion/styled";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { ReactNode, useState } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import IconButton from "../../button/IconButton";
import Dropzone, {
  ChildArgs,
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";

interface Props<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  children?: (args: ChildArgs<DroppableObject>) => ReactNode;
}

const Root = styled("div")<{
  isOver: boolean;
  isDroppable: boolean;
}>`
  background-color: ${({ theme, isDroppable, isOver }) => {
    if (isOver && isDroppable) return theme.col.grayLight;
    if (isDroppable) return theme.col.grayVeryLight;
    return "inherit";
  }};
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 1px;
  z-index: 2;
  position: relative;
`;

const PlusContainer = styled("div")`
  margin-left: 45%;
  width: 10%;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  children,
  onDrop,
}: Props<DroppableObject>) => {
  const [showDropzone, setShowDropzone] = useState(false);
  const [{ isOver, isDroppable }, drop] = useDrop({
    accept: acceptedDropTypes,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });
  const [{ isOver: isOver2 }, drop2] = useDrop({
    accept: acceptedDropTypes,
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  });

  const onDropped = (item: DroppableObject, monitor: DropTargetMonitor) => {
    setShowDropzone(false);
    onDrop(item, monitor);
  };

  return (
    <>
      {!(showDropzone || isOver || isOver2) && (
        <Root ref={drop} isOver={isOver} isDroppable={isDroppable}>
          <PlusContainer onClick={() => setShowDropzone(true)}>
            <IconButton icon={faPlus} />
          </PlusContainer>
        </Root>
      )}

      {(showDropzone || isOver || isOver2) && (
        // TODO x - to close the dropzone
        <Dropzone
          acceptedDropTypes={acceptedDropTypes}
          onDrop={onDropped}
          ref={drop2}
        >
          {children}
        </Dropzone>
      )}
    </>
  );
};

export default BetweenElements;
