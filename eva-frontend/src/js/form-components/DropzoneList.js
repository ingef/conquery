// @flow

import React from "react";
import styled from "@emotion/styled";

import IconButton from "conquery/lib/js/button/IconButton";
import Dropzone from "conquery/lib/js/form-components/Dropzone";
import Label from "conquery/lib/js/form-components/Label";

const ListItem = styled("div")`
  position: relative;
  padding: 5px;
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
  background-color: white;
  border: 1px solid ${({ theme }) => theme.col.blueGrayLight};
  &:not(:last-child) {
    border-bottom: 0;
  }
`;

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
`;

type PropsType = {
  className?: string,
  label?: string,
  dropzoneText: string,
  items: any,
  acceptedDropTypes: string[],
  onDrop: Function,
  onDelete: Function,
  disallowMultipleColumns?: boolean
};

const DropzoneList = (props: PropsType) => {
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
      {// allow at least one column
      ((props.items && props.items.length === 0) ||
        !props.disallowMultipleColumns) && (
        <Dropzone
          acceptedDropTypes={props.acceptedDropTypes}
          onDrop={props.onDrop}
        >
          {props.dropzoneText}
        </Dropzone>
      )}
    </div>
  );
};

export default DropzoneList;
