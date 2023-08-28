import styled from "@emotion/styled";
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

const Root = styled("div")`
  display: flex;
  height: 4px;
`;

const Line = styled("div")`
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin: 1px 0;
  height: 4px;
  border-radius: 2px;
  flex-grow: 1;
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
  return (
    <Root>
      <SxDropzone
        bare
        naked
        acceptedDropTypes={acceptedDropTypes}
        onDrop={onDrop}
        height={height}
        top={top}
      >
        {({ isOver }) => isOver && <Line />}
      </SxDropzone>
    </Root>
  );
};

export default DropzoneBetweenElements;
