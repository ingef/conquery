import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import { DNDType } from "../common/constants/dndTypes";
import { nodeIsConceptQueryNode } from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import Dropzone, { PossibleDroppableObject } from "../ui-components/Dropzone";

const SxDropzone = styled(Dropzone)`
  width: 100%;
`;

const DROP_TYPES = [DNDType.CONCEPT_TREE_NODE];

interface PropsT {
  node: DragItemConceptTreeNode;
  onDropConcept: (concept: DragItemConceptTreeNode) => void;
}

export const droppableObjectIsConceptTreeNode = (
  node: PossibleDroppableObject,
): node is DragItemConceptTreeNode => {
  return node.type === DNDType.CONCEPT_TREE_NODE;
};

export const canDropConceptTreeNodeBeDropped = (node: StandardQueryNodeT) => {
  return (item: PossibleDroppableObject) => {
    if (
      !droppableObjectIsConceptTreeNode(item) ||
      !nodeIsConceptQueryNode(node)
    ) {
      return false;
    }
    const conceptId = item.ids[0];
    const itemAlreadyInNode = node.ids.includes(conceptId);
    const itemHasConceptRoot = item.tree === node.tree;
    return itemHasConceptRoot && !itemAlreadyInNode;
  };
};

const ConceptDropzone: FC<PropsT> = ({ node, onDropConcept }) => {
  const { t } = useTranslation();

  return (
    <SxDropzone /* TOOD: ADD GENERIC TYPE <FC<DropzoneProps<DragItemConceptTreeNode>>> */
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => onDropConcept(item as DragItemConceptTreeNode)}
      canDrop={canDropConceptTreeNodeBeDropped(node)}
    >
      {() => t("queryNodeEditor.dropConcept")}
    </SxDropzone>
  );
};

export default ConceptDropzone;
