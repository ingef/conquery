import styled from "@emotion/styled";
import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { ReactNode, forwardRef, Ref, ReactElement, ForwardedRef } from "react";
import { DropTargetMonitor } from "react-dnd";

import IconButton from "../../button/IconButton";
import InfoTooltip from "../../tooltip/InfoTooltip";
import {
  ChildArgs,
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";
import DropzoneWithFileInput, {
  DragItemFile,
} from "../../ui-components/DropzoneWithFileInput";
import Label from "../../ui-components/Label";
import Optional from "../../ui-components/Optional";

import DropzoneBetweenElements from "./DropzoneBetweenElements";

const ListItem = styled("div")`
  position: relative;
  padding: 5px;
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
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

const ConceptContainer = styled("div")`
  position: relative;
`;

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
  onDrop: (
    props: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  onDropFile: (file: File) => void;
  onImportLines: (lines: string[]) => void;
  dropBetween: (
    i: number,
  ) => (item: DroppableObject, monitor: DropTargetMonitor) => void;
}

const DropzoneList = <DroppableObject extends PossibleDroppableObject>(
  {
    className,
    label,
    tooltip,
    optional,
    dropzoneChildren,
    items,
    acceptedDropTypes,
    onDelete,
    disallowMultipleColumns,
    onDrop,
    onImportLines,
    dropBetween,
  }: PropsT<DroppableObject>,
  ref: Ref<HTMLDivElement>,
) => {
  // allow at least one column
  const showDropzone =
    (items && items.length === 0) || !disallowMultipleColumns;

  function genItems() {
    return items.map((item, i) => (
      <ConceptContainer key={i}>
        {!disallowMultipleColumns && (
          <DropzoneBetweenElements
            acceptedDropTypes={acceptedDropTypes}
            onDrop={dropBetween(i)}
            top={-15}
          />
        )}
        <ListItem>
          <StyledIconButton icon={faTimes} onClick={() => onDelete(i)} />
          {item}
        </ListItem>
      </ConceptContainer>
    ));
  }

  return (
    <div className={className}>
      <Row>
        {label && (
          <Label>
            {optional && <Optional />}
            {label}
          </Label>
        )}
        {tooltip && <InfoTooltip text={tooltip} />}
      </Row>
      {items && items.length > 0 && (
        <>
          {genItems()}

          <ConceptContainer>
            {!disallowMultipleColumns && (
              <DropzoneBetweenElements
                acceptedDropTypes={acceptedDropTypes}
                onDrop={dropBetween(items.length)}
                top={-20}
                lastElement
              />
            )}
          </ConceptContainer>
        </>
      )}
      <div ref={ref}>
        {showDropzone && onImportLines && (
          <DropzoneWithFileInput
            acceptedDropTypes={acceptedDropTypes}
            onDrop={onDrop}
            onImportLines={onImportLines}
          >
            {dropzoneChildren}
          </DropzoneWithFileInput>
        )}
      </div>
    </div>
  );
};

export default forwardRef(DropzoneList) as <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>(
  props: PropsT<DroppableObject> & { ref?: ForwardedRef<HTMLDivElement> },
) => ReactElement;
