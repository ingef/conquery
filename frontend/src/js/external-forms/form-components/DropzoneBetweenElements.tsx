import styled from "@emotion/styled";
import { faPlus } from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { DropTargetMonitor, useDrop } from "react-dnd";
import { useTranslation } from "react-i18next";

import FaIcon from "../../icon/FaIcon";
import Dropzone, {
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";

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
  background-color: ${({ theme, isDroppable, isOver }) => {
    if (isOver && isDroppable) return theme.col.grayLight;
    return isDroppable ? theme.col.grayVeryLight : "inherit";
  }};
  margin-top: ${({ isFirstElement }) => (isFirstElement ? "5px" : "0px")};
  width: 100%;
  text-align: center;
`;

const PlusContainer = styled("div")`
  margin: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 21px;
`;

const DropzoneContainer = styled("div")`
  overflow: hidden;
  height: 54px;
`;

const SxFaIcon = styled(FaIcon)`
  height: 12px;
  color: ${({ theme }) => theme.col.black};
  opacity: 0.75;
`;

const BetweenElements = <DroppableObject extends PossibleDroppableObject>({
  acceptedDropTypes,
  onDrop: onDropCallback,
  isFirstElement,
}: Props<DroppableObject>) => {
  const { t } = useTranslation();

  const SxDropzone = styled(Dropzone<DroppableObject>)`
    margin: 5px 0 5px 0;
  `;

  const [showDropzone, setShowDropzone] = useState(false);

  const [{ isOver, isDroppable }, addZoneRef] = useDrop({
    accept: acceptedDropTypes,
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

  const onDrop = (item: DroppableObject, monitor: DropTargetMonitor) => {
    setShowDropzone(false);
    onDropCallback(item, monitor);
  };

  return (
    <>
      {!(showDropzone || isOver || isOver2) && (
        <Root
          ref={addZoneRef}
          isOver={isOver}
          isDroppable={isDroppable}
          isFirstElement={isFirstElement}
        >
          <PlusContainer onClick={() => setShowDropzone(true)}>
            <SxFaIcon icon={faPlus} />
          </PlusContainer>
        </Root>
      )}

      {(showDropzone || isOver || isOver2) && (
        <DropzoneContainer
          ref={dropzoneWrapperRef}
          onClick={() => setShowDropzone(false)}
        >
          <SxDropzone acceptedDropTypes={acceptedDropTypes} onDrop={onDrop}>
            {() => t("externalForms.default.dropBetweenLabel")}
          </SxDropzone>
        </DropzoneContainer>
      )}
    </>
  );
};

export default BetweenElements;
