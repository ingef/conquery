import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { DNDType } from "../common/constants/dndTypes";
import { canNodeBeDropped } from "../model/node";
import type { DragItemConceptTreeNode } from "../standard-query-editor/types";
import Dropzone from "../ui-components/Dropzone";

const SxDropzone = styled(Dropzone)`
  width: 100%;
`;

const DROP_TYPES = [DNDType.CONCEPT_TREE_NODE];

const ConceptDropzone = ({
  node,
  onDropConcept,
}: {
  node: DragItemConceptTreeNode;
  onDropConcept: (concept: DragItemConceptTreeNode) => void;
}) => {
  const { t } = useTranslation();

  return (
    <SxDropzone
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => onDropConcept(item as DragItemConceptTreeNode)}
      canDrop={(item) => canNodeBeDropped(node, item)}
    >
      {() => t("queryNodeEditor.dropConcept")}
    </SxDropzone>
  );
};

export default ConceptDropzone;
