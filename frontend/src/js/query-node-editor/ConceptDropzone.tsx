import React, { FC } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { CONCEPT_TREE_NODE } from "../common/constants/dndTypes";
import Dropzone from "../form-components/Dropzone";
import type {
  DraggedNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

const StyledDropzone = styled(Dropzone)`
  width: 100%;
`;

const DROP_TYPES = [CONCEPT_TREE_NODE];

interface PropsT {
  node: StandardQueryNodeT;
  onDropConcept: (concept: DraggedNodeType) => void;
}

const ConceptDropzone: FC<PropsT> = ({ node, onDropConcept }) => {
  const { t } = useTranslation();

  const dropzoneTarget = {
    // Usually, "drop" is specified here as well, but our Dropzone implementation splits that

    canDrop(_, monitor) {
      const item: DraggedNodeType = monitor.getItem();
      // The dragged item should contain exactly one id
      // since it was dragged from the tree
      const conceptId = item.ids[0];

      return (
        item.tree === node.tree && !node.ids.some((id) => id === conceptId)
      );
    },
  };

  const onDrop = (_, monitor) => {
    const item: DraggedNodeType = monitor.getItem();

    onDropConcept(item);
  };

  return (
    <StyledDropzone
      acceptedDropTypes={DROP_TYPES}
      onDrop={onDrop}
      target={dropzoneTarget}
    >
      {() => t("queryNodeEditor.dropConcept")}
    </StyledDropzone>
  );
};

export default ConceptDropzone;
