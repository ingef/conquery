import styled from "@emotion/styled";
import { memo } from "react";

import { ColumnDescription, ConceptIdT, DatasetT } from "../../api/types";
import { getConceptById } from "../../concept-trees/globalTreeStoreHelper";
import { EntityEvent } from "../reducer";

const Named = styled("span")`
  font-weight: bold;
`;

interface Props {
  datasetId: DatasetT["id"] | null;
  row: EntityEvent;
  column: ColumnDescription;
  rootConceptId: ConceptIdT;
}

const ConceptName = ({ datasetId, row, column, rootConceptId }: Props) => {
  // TODO: refactor. It's very implicit that the id is
  // somehow containing the datasetId.
  if (!datasetId) return null;

  const fullConceptId = `${datasetId}.${row[column.label]}`;
  const concept = getConceptById(fullConceptId, rootConceptId);

  if (!concept) {
    return <span>{row[column.label]}</span>;
  }

  const conceptName = (
    <Named>
      {concept
        ? `${concept.label}${
            concept.description ? " – " + concept.description : ""
          }`
        : row[column.label]}
    </Named>
  );

  if (fullConceptId === rootConceptId) {
    return conceptName;
  }

  const rootConcept = getConceptById(rootConceptId, rootConceptId);

  return (
    <div>
      {rootConcept ? `${rootConcept.label} ` : null}
      {conceptName}
    </div>
  );
};

export default memo(ConceptName);
