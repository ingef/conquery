import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import { ConceptBaseT, ConceptIdT } from "../api/types";
import { Heading4 } from "../headings/Headings";
import { DragItemConceptTreeNode } from "../standard-query-editor/types";

import ConceptDropzone from "./ConceptDropzone";
import ConceptEntry from "./ConceptEntry";
import { HeadingBetween } from "./HeadingBetween";

const Padded = styled("div")`
  padding: 0 15px 15px;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
`;
const Scrollable = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  height: 100%;
`;
const Heading4Highlighted = styled(Heading4)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin: 10px 0 5px;
`;

const AdditionalConceptNodeChildren = ({
  node,
  rootConcept,
  onRemoveConcept,
  onDropConcept,
}: {
  node: DragItemConceptTreeNode;
  rootConcept: ConceptBaseT;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
}) => {
  const { t } = useTranslation();

  const sortedNodeIds = [...node.ids].sort();

  return (
    <>
      <HeadingBetween>{t("queryNodeEditor.dropMoreConcepts")}</HeadingBetween>
      <Padded>
        <Heading4Highlighted>{rootConcept.label}</Heading4Highlighted>
        <div>
          <ConceptDropzone node={node} onDropConcept={onDropConcept} />
        </div>
        <Scrollable>
          {sortedNodeIds.map((conceptId) => (
            <ConceptEntry
              key={conceptId}
              conceptId={conceptId}
              root={rootConcept}
              canRemoveConcepts={node.ids.length > 1}
              onRemoveConcept={onRemoveConcept}
            />
          ))}
        </Scrollable>
      </Padded>
    </>
  );
};

export default memo(AdditionalConceptNodeChildren);
