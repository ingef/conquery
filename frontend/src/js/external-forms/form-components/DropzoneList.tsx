import React, { ReactNode } from "react";
import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";
import Dropzone, { ChildArgs } from "../../form-components/Dropzone";
import DropzoneWithFileInput from "../../form-components/DropzoneWithFileInput";
import Label from "../../form-components/Label";

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

interface PropsT {
  className?: string;
  label?: ReactNode;
  dropzoneChildren: (args: ChildArgs) => ReactNode;
  items: ReactNode[];
  acceptedDropTypes: string[];
  onDrop: (props: any, monitor: any) => void;
  onDropFile?: (file: File) => void;
  onDelete: (idx: number) => void;
  disallowMultipleColumns?: boolean;
}

const DropzoneList = (props: PropsT) => {
  // allow at least one column
  const showDropzone =
    (props.items && props.items.length === 0) || !props.disallowMultipleColumns;

  const DropzoneClass = props.onDropFile ? DropzoneWithFileInput : Dropzone;

  return (
    <div className={props.className}>
      {props.label && <Label>{props.label}</Label>}
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
