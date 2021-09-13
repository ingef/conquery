import styled from "@emotion/styled";
import React, { ReactNode } from "react";
import { DropTargetMonitor } from "react-dnd";

import IconButton from "../../button/IconButton";
import Dropzone, {
  ChildArgs,
  PossibleDroppableObject,
} from "../../form-components/Dropzone";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../../form-components/DropzoneWithFileInput";
import Label from "../../form-components/Label";
import Optional from "../../form-components/Optional";
import InfoTooltip from "../../tooltip/InfoTooltip";

const ListItem = styled("div")`
  position: relative;
  padding: 5px;
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
  margin-bottom: 5px;
`;

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

interface PropsT<DroppableObject> {
  className?: string;
  label?: ReactNode;
  tooltip?: string;
  optional?: boolean;
  dropzoneChildren: (args: ChildArgs) => ReactNode;
  items: ReactNode[];
  acceptedDropTypes: string[];
  onDrop: (
    props: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  onDropFile?: (file: File) => void;
  onDelete: (idx: number) => void;
  disallowMultipleColumns?: boolean;
}

const DropzoneList = <DroppableObject extends PossibleDroppableObject>(
  props: PropsT<DroppableObject>,
) => {
  // allow at least one column
  const showDropzone =
    (props.items && props.items.length === 0) || !props.disallowMultipleColumns;

  const DropzoneClass = props.onDropFile ? DropzoneWithFileInput : Dropzone;

  return (
    <div className={props.className}>
      <Row>
        {props.label && (
          <Label>
            {props.optional && <Optional />}
            {props.label}
          </Label>
        )}
        {props.tooltip && <InfoTooltip text={props.tooltip} />}
      </Row>
      {props.items && props.items.length > 0 && (
        <div>
          {props.items.map((item, i) => (
            <ListItem key={i}>
              <StyledIconButton
                icon="times"
                onClick={() => props.onDelete(i)}
              />
              {item}
            </ListItem>
          ))}
        </div>
      )}
      {showDropzone && (
        <DropzoneClass
          acceptedDropTypes={props.acceptedDropTypes}
          onDrop={props.onDrop}
          onSelectFile={props.onDropFile!}
        >
          {props.dropzoneChildren}
        </DropzoneClass>
      )}
    </div>
  );
};

export default DropzoneList;
