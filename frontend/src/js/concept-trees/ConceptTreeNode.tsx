import styled from "@emotion/styled";
import { FC } from "react";

import type {
  ConceptIdT,
  InfoT,
  DateRangeT,
  ConceptT,
  ConceptElementT,
} from "../api/types";
import { useOpenableConcept } from "../concept-trees-open/useOpenableConcept";
import { resetSelects } from "../model/select";
import { resetTables } from "../model/table";
import type { ConceptQueryNodeType } from "../standard-query-editor/types";

import ConceptTreeNodeTextContainer from "./ConceptTreeNodeTextContainer";
import { getConceptById } from "./globalTreeStoreHelper";
import type { SearchT } from "./reducer";
import { isNodeInSearchResult } from "./selectors";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

// Concept data that is necessary to display tree nodes. Includes additional infos
// for the tooltip as well as the id of the corresponding tree
interface TreeNodeData {
  label: string;
  description?: string;
  active?: boolean;
  matchingEntries: number | null;
  matchingEntities: number | null;
  dateRange?: DateRangeT;
  additionalInfos?: InfoT[];
  children?: ConceptIdT[];
}

interface PropsT {
  rootConceptId: ConceptIdT;
  conceptId: ConceptIdT;
  data: TreeNodeData;
  depth: number;
  search: SearchT;
}

const selectTreeNodeData = (concept: ConceptT) => ({
  label: concept.label,
  description: concept.description,
  active: concept.active,
  matchingEntries: concept.matchingEntries,
  matchingEntities: concept.matchingEntities,
  dateRange: concept.dateRange,
  additionalInfos: concept.additionalInfos,
  children: concept.children,
});

const ConceptTreeNode: FC<PropsT> = ({
  data,
  rootConceptId,
  conceptId,
  depth,
  search,
}) => {
  const { open, onToggleOpen } = useOpenableConcept({
    conceptId,
  });

  function toggleOpen() {
    if (!data.children) return;

    onToggleOpen();
  }

  if (!search.showMismatches) {
    const shouldRender = isNodeInSearchResult(conceptId, search, data.children);

    if (!shouldRender) return null;
  }

  const isOpen = open || search.allOpen;

  return (
    <Root>
      <ConceptTreeNodeTextContainer
        node={{
          label: data.label,
          description: data.description,

          additionalInfos: data.additionalInfos,
          matchingEntries: data.matchingEntries,
          matchingEntities: data.matchingEntities,
          dateRange: data.dateRange,

          children: data.children,
        }}
        conceptId={conceptId}
        createQueryElement={(): ConceptQueryNodeType => {
          const concept = getConceptById(
            rootConceptId,
            rootConceptId, // To optimize lookup
          ) as ConceptElementT | null;

          const description = data.description
            ? { description: data.description }
            : {};

          return {
            ids: [conceptId],
            ...description,
            label: data.label,
            tables: concept?.tables
              ? resetTables(concept.tables, { useDefaults: true })
              : [],
            selects: concept?.selects
              ? resetSelects(concept.selects, { useDefaults: true })
              : [],

            additionalInfos: data.additionalInfos,
            matchingEntries: data.matchingEntries,
            matchingEntities: data.matchingEntities,
            dateRange: data.dateRange,

            tree: rootConceptId,
          };
        }}
        open={isOpen}
        depth={depth}
        active={data.active}
        onTextClick={toggleOpen}
        search={search}
      />
      {!!data.children && isOpen && (
        <>
          {data.children.map((childId) => {
            const child = getConceptById(childId);

            return child ? (
              <ConceptTreeNode
                key={childId}
                rootConceptId={rootConceptId}
                conceptId={childId}
                data={selectTreeNodeData(child)}
                depth={depth + 1}
                search={search}
              />
            ) : null;
          })}
        </>
      )}
    </Root>
  );
};

export default ConceptTreeNode;
