import React from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";
import IconButton from "../button/IconButton";

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

const ConceptEntry = AdditionalInfoHoverable(
  ({ node, conceptId, canRemoveConcepts, onRemoveConcept }) => {
    const { t } = useTranslation();

    return (
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
            regular
            icon="trash-alt"
          />
        )}
      </Concept>
    );
  }
);

export default ConceptEntry;
