import { useMemo } from "react";
import type { ConceptIdT } from "../api/types";

import ConceptTree from "./ConceptTree";
import ConceptTreeFolder from "./ConceptTreeFolder";
import { getConceptById } from "./globalTreeStoreHelper";
import type { LoadedConcept, SearchT, TreesT } from "./reducer";
import { isNodeInSearchResult } from "./selectors";

const getNonFolderChildren = (trees: TreesT, node: LoadedConcept): string[] => {
  if (node.detailsAvailable) return node.children || [];

  if (!node.children) return [];

  // recursively get children of children
  return node.children.reduce((acc, childId) => {
    const child = trees[childId];
    return acc.concat(getNonFolderChildren(trees, child));
  }, [] as ConceptIdT[]);
};

const ConceptTreeListItem = ({
  trees,
  conceptId,
  search,
  onLoadTree,
}: {
  trees: TreesT;
  conceptId: ConceptIdT;
  search: SearchT;
  onLoadTree: (id: string) => void;
}) => {
  const tree = trees[conceptId];

  const nonFolderChildren = useMemo(() => {
    if (tree.detailsAvailable) return tree.children;

    return getNonFolderChildren(trees, tree);
  }, [trees, tree]);

  if (!isNodeInSearchResult(conceptId, search, nonFolderChildren)) return null;

  const rootConcept = getConceptById(conceptId);

  const commonProps = {
    conceptId,
    search,
    onLoadTree,
    depth: 0,
  };

  return tree.detailsAvailable ? (
    <ConceptTree
      label={tree.label}
      tree={rootConcept}
      loading={!!tree.loading}
      error={tree.error}
      {...commonProps}
    />
  ) : (
    <ConceptTreeFolder
      trees={trees}
      tree={tree}
      active={tree.active}
      openInitially
      {...commonProps}
    />
  );
};

export default ConceptTreeListItem;
