import styled from "@emotion/styled";
import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";

import { ConceptIdT, DatasetT } from "../../api/types";
import { getConceptById } from "../../concept-trees/globalTreeStoreHelper";
import FaIcon from "../../icon/FaIcon";

const Root = styled("div")`
  display: flex;
  align-items: center;
  gap: 10px;
`;
const Named = styled("span")`
  font-weight: 400;
`;

interface Props {
  className?: string;
  title?: string;
  conceptId: string;
  rootConceptId: ConceptIdT;
}

const ConceptName = ({ className, title, rootConceptId, conceptId }: Props) => {
  const concept = getConceptById(conceptId, rootConceptId);

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

  if (conceptId === rootConceptId) {
    return (
      <div title={title} className={className}>
        {conceptName}
      </div>
    );
  }

  const rootConcept = getConceptById(rootConceptId, rootConceptId);

  return (
    <Root title={title} className={className}>
      <FaIcon icon={faFolder} active />
      <span>
        {rootConcept ? `${rootConcept.label} ` : null}
        {conceptName}
      </span>
    </Root>
  );
};

export default memo(ConceptName);
