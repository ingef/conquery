import styled from "@emotion/styled";
import React, { FC } from "react";

import type { ConceptIdT, InfoT, DateRangeT, ConceptT } from "../api/types";
import { useOpenableConcept } from "../concept-trees-open/useOpenableConcept";
import { selectsWithDefaults } from "../model/select";
import { tablesWithDefaults } from "../model/table";
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
  description: string;
  active: boolean;
  matchingEntries: number;
  matchingEntities: number;
  dateRange: DateRangeT;
  additionalInfos: InfoT[];
  children: ConceptIdT[];

  tree: ConceptIdT;
}

interface PropsT {
  id: ConceptIdT;
  data: TreeNodeData;
  depth: number;
  search: SearchT;
}

const selectTreeNodeData = (concept: ConceptT, tree: ConceptIdT) => ({
  label: concept.label,
  description: concept.description,
  active: concept.active,
  matchingEntries: concept.matchingEntries,
  matchingEntities: concept.matchingEntities,
  dateRange: concept.dateRange,
  additionalInfos: concept.additionalInfos,
  children: concept.children,

  tree,
});

const ConceptTreeNode: FC<PropsT> = ({ data, id, depth, search }) => {
  const { open, onToggleOpen } = useOpenableConcept({
    conceptId: id,
  });

  function toggleOpen() {
    if (!data.children) return;

    onToggleOpen();
  }

  if (!search.showMismatches) {
    const shouldRender = isNodeInSearchResult(id, data.children, search);

    if (!shouldRender) return null;
  }

  const isOpen = open || search.allOpen;

  return (
    <Root>
      <ConceptTreeNodeTextContainer
        node={{
          id,
          label: data.label,
          description: data.description,

          additionalInfos: data.additionalInfos,
          matchingEntries: data.matchingEntries,
          matchingEntities: data.matchingEntities,
          dateRange: data.dateRange,

          children: data.children,
        }}
        createQueryElement={(): ConceptQueryNodeType => {
          const { tables, selects } = getConceptById(data.tree);

          const description = data.description
            ? { description: data.description }
            : {};

          return {
            ids: [id],
            ...description,
            label: data.label,
            tables: tablesWithDefaults(tables),
            selects: selectsWithDefaults(selects),

            additionalInfos: data.additionalInfos,
            matchingEntries: data.matchingEntries,
            matchingEntities: data.matchingEntities,
            dateRange: data.dateRange,

            tree: data.tree,
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
          {data.children.map((childId, i) => {
            const child = getConceptById(childId);

            return child ? (
              <ConceptTreeNode
                key={i}
                id={childId}
                data={selectTreeNodeData(child, data.tree)}
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
