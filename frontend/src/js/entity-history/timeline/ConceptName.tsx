import styled from "@emotion/styled";
import { memo } from "react";

import { ConceptIdT, DatasetT } from "../../api/types";
import { getConceptById } from "../../concept-trees/globalTreeStoreHelper";

const Named = styled("span")`
  font-weight: 400;
`;

interface Props {
  className?: string;
  title?: string;
  datasetId: DatasetT["id"] | null;
  conceptId: string; // Because it's just part of an actual ConceptT['id']
  rootConceptId: ConceptIdT;
}

const ConceptName = ({
  className,
  title,
  datasetId,
  rootConceptId,
  conceptId,
}: Props) => {
  // TODO: refactor. It's very implicit that the id is
  // somehow containing the datasetId.
  if (!datasetId) return null;

  const fullConceptId = `${datasetId}.${conceptId}`;
  const concept = getConceptById(fullConceptId, rootConceptId);

  if (!concept) {
    return (
      <span className={className} title={title}>
        {conceptId}
      </span>
    );
  }

  const conceptName = (
    <Named>
      {concept
        ? `${concept.label}${
            concept.description ? " – " + concept.description : ""
          }`
        : conceptId}
    </Named>
  );

  if (fullConceptId === rootConceptId) {
    return (
      <div title={title} className={className}>
        {conceptName}
      </div>
    );
  }

  const rootConcept = getConceptById(rootConceptId, rootConceptId);

  return (
    <div title={title} className={className}>
      {rootConcept ? `${rootConcept.label} ` : null}
      {conceptName}
    </div>
  );
};

export default memo(ConceptName);
