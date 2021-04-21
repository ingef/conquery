import styled from "@emotion/styled";
import React, { ReactNode, FC } from "react";

import IconButton from "../button/IconButton";

import EditableTagsForm from "./EditableTagsForm";

interface PropsT {
  className?: string;
  tags?: string[];
  editing: boolean;
  loading: boolean;
  tagComponent?: ReactNode;
  onSubmit: (value: string[]) => void;
  onToggleEdit: () => void;
  availableTags: string[];
}

const EditableTagsDisplay = styled("div")`
  display: flex;
  flex-direction: row;
`;
const StyledIconButton = styled(IconButton)`
  margin-right: 5px;
`;

const EditableTags: FC<PropsT> = (props) => {
  if (props.editing) {
    return (
      <EditableTagsForm
        className={props.className}
        tags={props.tags}
        loading={props.loading}
        onSubmit={props.onSubmit}
        onCancel={props.onToggleEdit}
        availableTags={props.availableTags}
      />
    );
  } else {
    if (!props.tags || props.tags.length === 0) return null;

    return (
      <EditableTagsDisplay className={props.className}>
        <StyledIconButton bare icon="edit" onClick={props.onToggleEdit} />
        {props.tagComponent}
      </EditableTagsDisplay>
    );
  }
};

export default EditableTags;
