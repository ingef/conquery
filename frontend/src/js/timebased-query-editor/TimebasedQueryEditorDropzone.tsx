import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { PREVIOUS_QUERY, TIMEBASED_NODE } from "../common/constants/dndTypes";
import Dropzone, { DropzoneProps } from "../form-components/Dropzone";
import type { DragItemQuery } from "../standard-query-editor/types";

import type { DragItemTimebasedNode } from "./TimebasedNode";
import { removeTimebasedNode } from "./actions";
import { TimebasedResultType } from "./reducer";

interface PropsType {
  onDropNode: (node: TimebasedResultType, moved: boolean) => void;
}

const StyledDropzone = styled(Dropzone)`
  width: 150px;
  text-align: center;
`;

const TimebasedQueryEditorDropzone = ({ onDropNode }: PropsType) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const onRemoveTimebasedNode = (
    conditionIdx: number,
    resultIdx: number,
    moved: boolean,
  ) => dispatch(removeTimebasedNode({ conditionIdx, resultIdx, moved }));

  const onDrop = (item) => {
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
    <StyledDropzone<FC<DropzoneProps<DragItemQuery | DragItemTimebasedNode>>>
      acceptedDropTypes={[PREVIOUS_QUERY, TIMEBASED_NODE]}
      onDrop={onDrop}
    >
      {() => t("dropzone.dragQuery")}
    </StyledDropzone>
  );
};

export default TimebasedQueryEditorDropzone;
