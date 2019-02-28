// @flow

import React from "react";
import styled from "@emotion/styled";

import SelectableLabel from "../selectable-label/SelectableLabel";
import EditableTextForm from "./EditableTextForm";
import IconButton from "../button/IconButton";

type PropsType = {
  className?: string,
  loading: boolean,
  editing: boolean,
  text: string,
  selectTextOnMount: boolean,
  onSubmit: () => void,
  onToggleEdit: () => void
};

const StyledIconButton = styled(IconButton)`
  margin-right: 5px;
`;

const Text = styled("p")`
  margin: 0;
`;

class EditableText extends React.Component<PropsType> {
  render() {
    return this.props.editing ? (
      <EditableTextForm
        className={this.props.className}
        loading={this.props.loading}
        text={this.props.text}
        selectTextOnMount={this.props.selectTextOnMount}
        onSubmit={this.props.onSubmit}
        onCancel={this.props.onToggleEdit}
      />
    ) : (
      <Text className={this.props.className}>
        <StyledIconButton
          large
          bare
          icon="edit"
          onClick={this.props.onToggleEdit}
        />
        <SelectableLabel label={this.props.text} />
      </Text>
    );
  }
}

export default EditableText;
