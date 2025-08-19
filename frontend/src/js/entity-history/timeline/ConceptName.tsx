import styled from "@emotion/styled";
import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";

import Highlighter from "react-highlight-words";
import { ConceptIdT, ConceptT } from "../../api/types";
import { getConceptById } from "../../concept-trees/globalTreeStoreHelper";
import FaIcon from "../../icon/FaIcon";
import { useTimelineSearch } from "../timeline-search/timelineSearchState";

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

const ConceptLabel = ({
  conceptId,
  concept,
  searchTerm,
}: {
  conceptId: string;
  concept?: ConceptT;
  searchTerm?: string;
}) => {
  const label = concept
    ? `${concept.label}${
        concept.description ? " – " + concept.description : ""
      }`
    : conceptId;

  return (
    <Named>
      {searchTerm && searchTerm.length > 0 ? (
        <Highlighter
          searchWords={searchTerm.split(" ")}
          textToHighlight={label}
        />
      ) : (
        label
      )}
    </Named>
  );
};

const RootConceptLabel = ({
  rootConcept,
  searchTerm,
}: {
  rootConcept: ConceptT;
  searchTerm?: string;
}) => {
  return searchTerm && searchTerm.length > 0 ? (
    <Highlighter
      searchWords={searchTerm.split(" ")}
      textToHighlight={rootConcept.label + " "}
    />
  ) : (
    rootConcept.label + " "
  );
};

const ConceptName = ({ className, title, rootConceptId, conceptId }: Props) => {
  const { searchTerm } = useTimelineSearch();
  const concept = getConceptById(conceptId, rootConceptId);

  if (!concept) {
    return (
      <span className={className} title={title}>
        {conceptId}
      </span>
    );
  }

  if (conceptId === rootConceptId) {
    return (
      <div title={title} className={className}>
        <ConceptLabel
          conceptId={conceptId}
          concept={concept}
          searchTerm={searchTerm}
        />
      </div>
    );
  }

  const rootConcept = getConceptById(rootConceptId, rootConceptId);

  return (
    <Root title={title} className={className}>
      <FaIcon icon={faFolder} active />
      <span>
        {rootConcept && (
          <RootConceptLabel rootConcept={rootConcept} searchTerm={searchTerm} />
        )}
        <ConceptLabel
          conceptId={conceptId}
          concept={concept}
          searchTerm={searchTerm}
        />
      </span>
    </Root>
  );
};

export default memo(ConceptName);
