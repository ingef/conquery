import styled from "@emotion/styled";
import { ReactNode, forwardRef, Ref, ReactElement, ForwardedRef } from "react";
import { DropTargetMonitor } from "react-dnd";

import IconButton from "../../button/IconButton";
import InfoTooltip from "../../tooltip/InfoTooltip";
import Dropzone, {
  ChildArgs,
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../../ui-components/DropzoneWithFileInput";
import Label from "../../ui-components/Label";
import Optional from "../../ui-components/Optional";

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

interface WithFileProps<DroppableObject> extends PropsT<DroppableObject> {
  onDrop: (
    props: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  onDropFile: (file: File) => void;
}

interface WithoutFileProps<DroppableObject> extends PropsT<DroppableObject> {
  onDrop: (props: DroppableObject, monitor: DropTargetMonitor) => void;
  onDropFile: undefined;
}

interface PropsT<DroppableObject> {
  className?: string;
  label?: ReactNode;
  tooltip?: string;
  optional?: boolean;
  dropzoneChildren: (args: ChildArgs<DroppableObject>) => ReactNode;
  items: ReactNode[];
  acceptedDropTypes: string[];
  onDelete: (idx: number) => void;
  disallowMultipleColumns?: boolean;
}

const DropzoneList = <DroppableObject extends PossibleDroppableObject>(
  props: WithFileProps<DroppableObject> | WithoutFileProps<DroppableObject>,
  ref: Ref<HTMLDivElement>,
) => {
  // allow at least one column
  const showDropzone =
    (props.items && props.items.length === 0) || !props.disallowMultipleColumns;

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
      <div ref={ref}>
        {showDropzone &&
          (props.onDropFile ? (
            <DropzoneWithFileInput
              acceptedDropTypes={props.acceptedDropTypes}
              onDrop={props.onDrop}
              onSelectFile={props.onDropFile}
            >
              {props.dropzoneChildren}
            </DropzoneWithFileInput>
          ) : (
            <Dropzone
              acceptedDropTypes={props.acceptedDropTypes}
              onDrop={props.onDrop}
            >
              {props.dropzoneChildren}
            </Dropzone>
          ))}
      </div>
    </div>
  );
};

export default forwardRef(DropzoneList) as <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>(
  props: (
    | WithFileProps<DroppableObject>
    | WithoutFileProps<DroppableObject>
  ) & { ref?: ForwardedRef<HTMLDivElement> },
) => ReactElement;
