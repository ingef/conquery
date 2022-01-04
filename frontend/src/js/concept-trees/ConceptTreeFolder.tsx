import styled from "@emotion/styled";
import { FC } from "react";

import type { ConceptT, ConceptIdT } from "../api/types";
import { useOpenableConcept } from "../concept-trees-open/useOpenableConcept";

import ConceptTree from "./ConceptTree";
import ConceptTreeNodeTextContainer from "./ConceptTreeNodeTextContainer";
import { getConceptById } from "./globalTreeStoreHelper";
import type { SearchT, TreesT } from "./reducer";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

interface PropsT {
  depth: number;
  trees: TreesT;
  tree: ConceptT;
  conceptId: ConceptIdT;
  active?: boolean;
  openInitially?: boolean;
  search: SearchT;
  onLoadTree: (id: string) => void;
}

const sumMatchingEntries = (children: string[], initSum: number) => {
  return children.reduce((sum, treeId) => {
    const rootConcept = getConceptById(treeId);
    const rootMatchingEntries = rootConcept ? rootConcept.matchingEntries : 0;

    return rootMatchingEntries ? sum + rootMatchingEntries : sum;
  }, initSum);
};

const ConceptTreeFolder: FC<PropsT> = ({
  trees,
  tree,
  conceptId,
  search,
  depth,
  active,
  onLoadTree,
  openInitially,
}) => {
  const { open, onToggleOpen } = useOpenableConcept({
    conceptId,
    openInitially,
  });

  const matchingEntries =
    !tree.children || !tree.matchingEntries
      ? null
      : sumMatchingEntries(tree.children, tree.matchingEntries);

  const isOpen = open || search.allOpen;

  return (
    <Root>
      <ConceptTreeNodeTextContainer
        node={{
          label: tree.label,
          description: tree.description,
          matchingEntries: matchingEntries,
          dateRange: tree.dateRange,
          additionalInfos: tree.additionalInfos,
          children: tree.children,
          matchingEntities: tree.matchingEntities,
        }}
        conceptId={conceptId}
        isStructFolder
        open={open || false}
        depth={depth}
        active={active}
        onTextClick={onToggleOpen}
        search={search}
      />
      {isOpen &&
        tree.children &&
        tree.children.map((childId) => {
          const tree = trees[childId];

          const treeProps = {
            key: childId,
            conceptId: childId as ConceptIdT,
            depth: depth + 1,
            search,
            onLoadTree,
          };

          if (tree.detailsAvailable) {
            const rootConcept = getConceptById(childId);

            return (
              <ConceptTree
                label={tree.label}
                error={tree.error}
                loading={tree.loading}
                tree={rootConcept}
                {...treeProps}
              />
            );
          } else {
            const treeFolderProps = {
              tree,
              trees: trees,
              openInitially: false,
              active: tree.active,
            };

            return <ConceptTreeFolder {...treeFolderProps} {...treeProps} />;
          }
        })}
    </Root>
  );
};

export default ConceptTreeFolder;
