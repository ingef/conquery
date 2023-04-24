import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import { DNDType } from "../common/constants/dndTypes";
import { canNodeBeDropped } from "../model/node";
import type { DragItemConceptTreeNode } from "../standard-query-editor/types";
import Dropzone from "../ui-components/Dropzone";

const SxDropzone = styled(Dropzone)`
  width: 100%;
`;

const DROP_TYPES = [DNDType.CONCEPT_TREE_NODE];

interface PropsT {
  node: DragItemConceptTreeNode;
  onDropConcept: (concept: DragItemConceptTreeNode) => void;
}

const ConceptDropzone: FC<PropsT> = ({ node, onDropConcept }) => {
  const { t } = useTranslation();

  return (
    <SxDropzone /* TOOD: ADD GENERIC TYPE <FC<DropzoneProps<DragItemConceptTreeNode>>> */
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => onDropConcept(item as DragItemConceptTreeNode)}
      canDrop={(item) => canNodeBeDropped(node, item)}
    >
      {() => t("queryNodeEditor.dropConcept")}
    </SxDropzone>
  );
};

export default ConceptDropzone;
