import styled from "@emotion/styled";
import React, { ReactNode, FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

import EditableTagsForm from "./EditableTagsForm";

interface PropsT {
  className?: string;
  tags?: string[];
  loading: boolean;
  tagComponent?: ReactNode;
  onSubmit: (value: string[]) => Promise<any>;
  isEditing: boolean;
  setIsEditing: (value: boolean) => void;
  availableTags: string[];
}

const EditableTagsDisplay = styled("div")`
  display: flex;
  flex-direction: row;
`;
const SxIconButton = styled(IconButton)`
  margin-right: 5px;
  margin-top: 2px;
`;

const EditableTags: FC<PropsT> = (props) => {
  const { t } = useTranslation();

  if (props.isEditing) {
    return (
      <EditableTagsForm
        className={props.className}
        tags={props.tags}
        loading={props.loading}
        onSubmit={async (tags) => {
          try {
            await props.onSubmit(tags);
            props.setIsEditing(false);
          } catch (e) {}
        }}
        onCancel={() => props.setIsEditing(false)}
        availableTags={props.availableTags}
      />
    );
  } else {
    if (!props.tags || props.tags.length === 0) return null;

    return (
      <EditableTagsDisplay className={props.className}>
        <WithTooltip text={t("common.edit")}>
          <SxIconButton
            bare
            icon="folder"
            onClick={() => props.setIsEditing(true)}
          />
        </WithTooltip>
        {props.tagComponent}
      </EditableTagsDisplay>
    );
  }
};

export default EditableTags;
