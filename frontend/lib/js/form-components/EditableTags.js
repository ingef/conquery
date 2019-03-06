// @flow

import * as React from "react";
import styled from "@emotion/styled";

import { Tags } from "../tags";
import EditableTagsForm from "./EditableTagsForm";
import IconButton from "../button/IconButton";

type PropsType = {
  className: string,
  tags?: string[],
  editing: boolean,
  loading: boolean,
  tagComponent?: React.Node,
  onSubmit: () => void,
  onToggleEdit: () => void,
  availableTags: string[]
};

const EditableTagsDisplay = styled("div")`
  display: flex;
  flex-direction: row;
`;
const StyledIconButton = styled(IconButton)`
  margin-right: 3px;
`;

const StyledTags = styled(Tags)`
  display: inline-block;
`;

const EditableTags = (props: PropsType) => {
  return props.editing ? (
    <EditableTagsForm
      className={props.className}
      tags={props.tags}
      loading={props.loading}
      onSubmit={props.onSubmit}
      onCancel={props.onToggleEdit}
      availableTags={props.availableTags}
    />
  ) : (
    !!props.tags && props.tags.length > 0 && (
      <EditableTagsDisplay className={props.className}>
        <StyledIconButton large bare icon="edit" onClick={props.onToggleEdit} />
        {props.tagComponent || <StyledTags tags={props.tags} />}
      </EditableTagsDisplay>
    )
  );
};

export default EditableTags;
