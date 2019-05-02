// @flow

import React from "react";
import styled from "@emotion/styled";

import { AdditionalInfoHoverable } from "../tooltip";
import IconButton from "../button/IconButton";

const Concept = styled("div")`
  border-left: 3px solid ${({ theme }) => theme.col.blueGrayDark};
  padding-left: 10px;
  display: flex;
  flex-direction: row;
  margin-top: 10px;
`;

const ConceptContainer = styled("div")`
  flex-grow: 1;
`;

const ConceptEntryHeadline = styled("h6")`
  margin: 10px 0 10px 0;
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
`;

const ConceptEntryDescription = styled("p")`
  font-size: ${({ theme }) => theme.font.xs};
  margin: -10px 0 10px 0;
`;

const ConceptEntry = AdditionalInfoHoverable(
  ({ node, canRemoveConcepts, onRemoveConcept, conceptId }) => (
    <Concept>
      <ConceptContainer>
        <ConceptEntryHeadline>{node.label}</ConceptEntryHeadline>
        {node.description && (
          <ConceptEntryDescription>{node.description}</ConceptEntryDescription>
        )}
      </ConceptContainer>
      {canRemoveConcepts && (
        <IconButton
          onClick={() => onRemoveConcept(conceptId)}
          regular
          icon="trash-alt"
        />
      )}
    </Concept>
  )
);

export default ConceptEntry;
