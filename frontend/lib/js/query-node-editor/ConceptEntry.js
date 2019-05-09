// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { getConceptById } from "../category-trees/globalTreeStoreHelper";

import { AdditionalInfoHoverable } from "../tooltip";
import IconButton from "../button/IconButton";

const Concept = styled("div")`
  background-color: white;
  border: 1px solid ${({ theme }) => theme.col.gray};
  padding: 5px 15px;
  border-radius: 3px;
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

const ConceptEntry = AdditionalInfoHoverable(
  ({ node, conceptId, canRemoveConcepts, onRemoveConcept }) => {
    const concept = getConceptById(conceptId);

    return (
      <Concept>
        <ConceptContainer>
          {!concept ? (
            <NotFound>{T.translate("queryNodeEditor.nodeNotFound")}</NotFound>
          ) : (
            <>
              <ConceptEntryHeadline>{concept.label}</ConceptEntryHeadline>
              {concept.description && (
                <ConceptEntryDescription>
                  {concept.description}
                </ConceptEntryDescription>
              )}
            </>
          )}
        </ConceptContainer>
        {canRemoveConcepts && (
          <IconButton
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
