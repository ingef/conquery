import styled from "@emotion/styled";
import React from "react";

import IconButton from "../button/IconButton";
import HighlightableLabel from "../highlightable-label/HighlightableLabel";
import WithTooltip from "../tooltip/WithTooltip";

import EditableTextForm from "./EditableTextForm";

interface PropsT {
  className?: string;
  loading: boolean;
  editing: boolean;
  text: string;
  tooltip?: string;
  large?: boolean;
  saveOnClickoutside?: boolean;
  isHighlighted?: boolean;
  selectTextOnMount?: boolean;
  onSubmit: (text: string) => void;
  onToggleEdit: () => void;
}

const SxIconButton = styled(IconButton)`
  margin-right: ${({ large }) => (large ? "10px" : "7px")};
  padding: 2px 0;
`;

const Text = styled("div")`
  padding: 2px 0 0;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
`;

const SxHighlightableLabel = styled(HighlightableLabel)`
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

const EditableText: React.FC<PropsT> = (props) => {
  return props.editing ? (
    <EditableTextForm
      className={props.className}
      loading={props.loading}
      text={props.text}
      selectTextOnMount={props.selectTextOnMount}
      saveOnClickoutside={props.saveOnClickoutside}
      onSubmit={props.onSubmit}
      onCancel={props.onToggleEdit}
    />
  ) : (
    <Text className={props.className}>
      <WithTooltip text={props.tooltip}>
        <SxIconButton
          large={props.large}
          bare
          icon="pen"
          onClick={props.onToggleEdit}
        />
      </WithTooltip>
      <SxHighlightableLabel
        label={props.text}
        isHighlighted={props.isHighlighted}
      />
    </Text>
  );
};

export default EditableText;
