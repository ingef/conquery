import styled from "@emotion/styled";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { ReactNode, useState } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import FaIcon from "../../icon/FaIcon";
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
    return isDroppable ? theme.col.grayVeryLight : "inherit";
  }};
  width: 100%;
  text-align: center;
`;

const PlusContainer = styled("div")`
  margin: auto;
`;

const SxFaIcon = styled(FaIcon)`
  height: 15px;
  color: ${({ theme }) => theme.col.black};
  opacity: 0.75;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  children,
  onDrop,
}: Props<DroppableObject>) => {
  const SxDropzone = styled(Dropzone<DroppableObject>)`
    margin: 5px 0 5px 0;
  `;

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
            <SxFaIcon icon={faPlus} />
          </PlusContainer>
        </Root>
      )}

      {(showDropzone || isOver || isOver2) && (
        // TODO x - to close the dropzone
        <SxDropzone
          acceptedDropTypes={acceptedDropTypes}
          onDrop={onDropped}
          ref={drop2}
        >
          {children}
        </SxDropzone>
      )}
    </>
  );
};

export default BetweenElements;
