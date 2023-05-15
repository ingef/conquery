import styled from "@emotion/styled";
import { ReactNode } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";

import { DNDType } from "../../common/constants/dndTypes";
import Dropzone, {
  ChildArgs,
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";

interface PropsT<DroppableObject> {
  onDrop: (item: DroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  children?: (args: ChildArgs<DroppableObject>) => ReactNode;
}

const DropzoneBetweenElements = <
  DroppableObject extends PossibleDroppableObject,
>({
  onDrop,
  children,
  acceptedDropTypes,
}: PropsT<DroppableObject>) => {
  const Root = styled("div")<{
    isHovered: boolean;
  }>`
    width: 100%;
    left: 0;
    top: -17px;
    right: 0;
    position: absolute;
    bottom: 90%;
    border-radius: ${({ theme }) => theme.borderRadius};
  `;

  const DropzoneRoot = styled("div")`
    width: 100%;
    left: 0;
    top: -17px;
    right: 0;
    position: absolute;
    bottom: 90%;
    z-index: 2;
    background-color: ${({ theme }) => theme.col.bg};
  `;

  const [{ isOver, isDroppable }, drop] = useDrop({
    accept: [
      DNDType.FORM_CONFIG,
      DNDType.CONCEPT_TREE_NODE,
      DNDType.PREVIOUS_QUERY,
      DNDType.PREVIOUS_SECONDARY_ID_QUERY,
    ],
    hover: (_, __) => {
      if (!isDroppable) return;
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  const [{ isOver: isOver2, isDroppable: isDroppable2 }, drop2] = useDrop({
    accept: [
      DNDType.FORM_CONFIG,
      DNDType.CONCEPT_TREE_NODE,
      DNDType.PREVIOUS_QUERY,
      DNDType.PREVIOUS_SECONDARY_ID_QUERY,
    ],
    hover: (_, __) => {
      if (!isDroppable2) return;
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      isDroppable: monitor.canDrop(),
    }),
  });

  return (
    <>
      {!isOver && !isOver2 && <Root isHovered={isOver} ref={drop} />}
      {/* Show when hovered with text and dropzone */}
      {(isOver || isOver2) && (
        <DropzoneRoot ref={drop2}>
          <Dropzone onDrop={onDrop} acceptedDropTypes={acceptedDropTypes}>
            {children}
          </Dropzone>
        </DropzoneRoot>
      )}
    </>
  );
};

export default DropzoneBetweenElements;
