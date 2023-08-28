import styled from "@emotion/styled";
import { DropTargetMonitor } from "react-dnd";

import Dropzone, {
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";

const LINE_HEIGHT = 4;

const Line = styled("div")`
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  height: ${LINE_HEIGHT}px;
  width: 100%;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SxDropzone = styled(Dropzone)`
  height: 30px;
  position: absolute;
  top: 0;
  left: 0;
  transform: translateY(calc(-50% - ${LINE_HEIGHT / 2}px));
  z-index: 10;
  background-color: transparent;
`;

const DropzoneBetweenElements = ({
  acceptedDropTypes,
  onDrop,
}: {
  onDrop: (props: PossibleDroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
}) => {
  return (
    <SxDropzone
      bare
      naked
      acceptedDropTypes={acceptedDropTypes}
      onDrop={onDrop}
    >
      {({ isOver }) => isOver && <Line />}
    </SxDropzone>
  );
};

export default DropzoneBetweenElements;
