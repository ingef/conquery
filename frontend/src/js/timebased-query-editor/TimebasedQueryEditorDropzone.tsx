import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";

import { PREVIOUS_QUERY, TIMEBASED_NODE } from "../common/constants/dndTypes";
import Dropzone from "../form-components/Dropzone";

import { removeTimebasedNode } from "./actions";

type PropsType = {
  onDropNode: () => void;
  onRemoveTimebasedNode: () => void;
};

const StyledDropzone = styled(Dropzone)`
  width: 150px;
  text-align: center;
  background-color: ${({ theme }) => theme.col.bg};
`;

const DROP_TYPES = [PREVIOUS_QUERY, TIMEBASED_NODE];

const TimebasedQueryEditorDropzone = ({
  onRemoveTimebasedNode,
  onDropNode,
}: PropsType) => {
  const { t } = useTranslation();
  const onDrop = (props, monitor) => {
    const item = monitor.getItem();

    const { moved } = item;

    if (moved) {
      const { conditionIdx, resultIdx } = item;

      onRemoveTimebasedNode(conditionIdx, resultIdx, moved);
      onDropNode(item.node, moved);
    } else {
      onDropNode(item, false);
    }
  };

  return (
    <StyledDropzone acceptedDropTypes={DROP_TYPES} onDrop={onDrop}>
      {() => t("dropzone.dragQuery")}
    </StyledDropzone>
  );
};

export default connect(
  () => ({}),
  (dispatch) => ({
    onRemoveTimebasedNode: (conditionIdx, resultIdx, moved) =>
      dispatch(removeTimebasedNode(conditionIdx, resultIdx, moved)),
  }),
)(TimebasedQueryEditorDropzone);
