import * as React from "react";
import styled from "@emotion/styled";

import Tags from "../tags/Tags";
import EditableTagsForm from "./EditableTagsForm";
import IconButton from "../button/IconButton";

type PropsType = {
  className?: string;
  tags?: string[];
  editing: boolean;
  loading: boolean;
  tagComponent?: React.ReactNode;
  onSubmit: (value: string[]) => void;
  onToggleEdit: () => void;
  availableTags: string[];
};

const EditableTagsDisplay = styled("div")`
  display: flex;
  flex-direction: row;
`;
const StyledIconButton = styled(IconButton)`
  margin-right: 5px;
`;

const StyledTags = styled(Tags)`
  display: inline-block;
`;

const EditableTags = (props: PropsType) => {
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
        {props.tagComponent || <StyledTags tags={props.tags} />}
      </EditableTagsDisplay>
    );
  }
};

export default EditableTags;
