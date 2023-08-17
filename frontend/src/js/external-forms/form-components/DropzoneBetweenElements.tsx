import styled from "@emotion/styled";
import { useState } from "react";
import { DropTargetMonitor } from "react-dnd";

import Dropzone, {
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";

interface Props {
  onDrop: (props: PossibleDroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  top: number;
  height: number;
}

const LineHeight = 3;

const Line = styled("div")<{ show: boolean }>`
  overflow: hidden;
  display: block;
  visibility: ${({ show }) => (show ? "visible" : "hidden")};
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin: 1px 0;
  height: ${LineHeight}px;
  border-radius: 2px;
`;

const SxDropzone = styled(Dropzone)<{ height: number; top: number }>`
  height: ${({ height }) => height}px;
  top: ${({ top }) => top}px;
  position: absolute;
  background-color: transparent;
`;

const DropzoneBetweenElements = ({
  acceptedDropTypes,
  onDrop,
  height,
  top,
}: Props) => {
  let [isOver, setIsOver] = useState<boolean>(false);

  return (
    <>
      <Line show={isOver} />
      <SxDropzone
        bare
        naked
        acceptedDropTypes={acceptedDropTypes}
        onDrop={onDrop}
        setIsOver={setIsOver}
        height={height}
        top={top}
      />
    </>
  );
};

export default DropzoneBetweenElements;
