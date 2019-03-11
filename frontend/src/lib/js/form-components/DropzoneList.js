// @flow

import React from "react";
import styled from "@emotion/styled";

import IconButton from "../button/IconButton";
import Dropzone from "./Dropzone";
import Label from "./Label";

const ListItem = styled("div")`
  position: relative;
`;

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
`;

type PropsType = {
  className?: string,
  itemClassName?: string,
  dropzoneClassName?: string,
  label?: string,
  dropzoneText: string,
  items: any,
  acceptedDropTypes: string[],
  onDrop: Function,
  onDelete: Function,
  disallowMultipleColumns?: boolean
};

const DropzoneList = (props: PropsType) => {
  const FreeDropzone = Dropzone(null, props.acceptedDropTypes, props.onDrop);

  return (
    <div className={props.className}>
      {props.label && <Label>{props.label}</Label>}
      {props.items && props.items.length > 0 && (
        <div>
          {props.items.map((item, i) => (
            <ListItem key={i} className={props.itemClassName}>
              <StyledIconButton
                icon="close"
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
        <FreeDropzone
          className={props.dropzoneClassName}
          containsItem={false}
          dropzoneText={props.dropzoneText}
        />
      )}
    </div>
  );
};

export default DropzoneList;
