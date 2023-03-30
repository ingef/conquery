import styled from "@emotion/styled";
import { faTrashAlt } from "@fortawesome/free-regular-svg-icons";
import { useTranslation } from "react-i18next";

import type { ConceptIdT, ConceptT } from "../api/types";
import IconButton from "../button/IconButton";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";

const Concept = styled("div")`
  background-color: white;
  border: 1px solid ${({ theme }) => theme.col.gray};
  padding: 5px 15px;
  border-radius: ${({ theme }) => theme.borderRadius};
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-top: 5px;
`;

const ConceptContainer = styled("div")`
  flex-grow: 1;
`;

const ConceptEntryHeadline = styled("h6")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
`;

const ConceptEntryDescription = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.xs};
`;

const NotFound = styled(ConceptEntryHeadline)`
  color: ${({ theme }) => theme.col.red};
`;

const SxIconButton = styled(IconButton)`
  flex-shrink: 0;
`;

interface Props {
  conceptId: ConceptIdT;
  root: ConceptT;
  canRemoveConcepts?: boolean;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
}

const ConceptEntry = ({
  conceptId,
  root,
  canRemoveConcepts,
  onRemoveConcept,
}: Props) => {
  const { t } = useTranslation();
  const node = getConceptById(conceptId);

  const ConceptEntryRoot = (
    <Concept>
      <ConceptContainer>
        {!node ? (
          <NotFound>{t("queryNodeEditor.nodeNotFound")}</NotFound>
        ) : (
          <>
            <ConceptEntryHeadline>{node.label}</ConceptEntryHeadline>
            {node.description && (
              <ConceptEntryDescription>
                {node.description}
              </ConceptEntryDescription>
            )}
          </>
        )}
      </ConceptContainer>
      {canRemoveConcepts && (
        <SxIconButton
          onClick={() => onRemoveConcept(conceptId)}
          tiny
          icon={faTrashAlt}
        />
      )}
    </Concept>
  );

  return node && root ? (
    <AdditionalInfoHoverable node={node} root={root}>
      {ConceptEntryRoot}
    </AdditionalInfoHoverable>
  ) : (
    ConceptEntryRoot
  );
};

export default ConceptEntry;
