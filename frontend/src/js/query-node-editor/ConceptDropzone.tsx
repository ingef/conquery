import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import { CONCEPT_TREE_NODE } from "../common/constants/dndTypes";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import Dropzone, { DropzoneProps } from "../ui-components/Dropzone";

const StyledDropzone = styled(Dropzone)`
  width: 100%;
`;

const DROP_TYPES = [CONCEPT_TREE_NODE];

interface PropsT {
  node: StandardQueryNodeT;
  onDropConcept: (concept: DragItemConceptTreeNode) => void;
}

const ConceptDropzone: FC<PropsT> = ({ node, onDropConcept }) => {
  const { t } = useTranslation();

  return (
    <StyledDropzone<FC<DropzoneProps<DragItemConceptTreeNode>>>
      acceptedDropTypes={DROP_TYPES}
      onDrop={onDropConcept}
      canDrop={(item) => {
        // The dragged item should contain exactly one id
        // since it was dragged from the tree
        const conceptId = item.ids[0];

        return (
          item.tree === node.tree && !node.ids.some((id) => id === conceptId)
        );
      }}
    >
      {() => t("queryNodeEditor.dropConcept")}
    </StyledDropzone>
  );
};

export default ConceptDropzone;
