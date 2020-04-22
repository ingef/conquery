import React from "react";
import styled from "@emotion/styled";

import SelectableLabel from "../selectable-label/SelectableLabel";
import EditableTextForm from "./EditableTextForm";
import IconButton from "../button/IconButton";

interface PropsT {
  className?: string;
  loading: boolean;
  editing: boolean;
  text: string;
  large?: boolean;
  selectTextOnMount?: boolean;
  onSubmit: (text: string) => void;
  onToggleEdit: () => void;
}

const StyledIconButton = styled(IconButton)`
  margin-top: 1px;
  margin-right: ${({ large }) => (large ? "10px" : "5px")};
  padding: 2px 0;
`;

const Text = styled("p")`
  margin: 0;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
`;

const EditableText: React.FC<PropsT> = (props) => {
  return props.editing ? (
    <EditableTextForm
      className={props.className}
      loading={props.loading}
      text={props.text}
      selectTextOnMount={props.selectTextOnMount}
      onSubmit={props.onSubmit}
      onCancel={props.onToggleEdit}
    />
  ) : (
    <Text className={props.className}>
      <StyledIconButton
        large={props.large}
        bare
        icon="edit"
        onClick={props.onToggleEdit}
      />
      <SelectableLabel label={props.text} />
    </Text>
  );
};

export default EditableText;
