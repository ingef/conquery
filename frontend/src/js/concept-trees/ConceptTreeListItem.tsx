import * as React from "react";

import type { ConceptT, ConceptIdT } from "../api/types";

import ConceptTree from "./ConceptTree";
import ConceptTreeFolder from "./ConceptTreeFolder";

import { getConceptById } from "./globalTreeStoreHelper";

import { isNodeInSearchResult } from "./selectors";

import type { TreesT, SearchT } from "./reducer";

type PropsT = {
  trees: TreesT;
  tree: ConceptT;
  treeId: ConceptIdT;
  search: SearchT;
  onLoadTree: (id: string) => void;
};

export default ({ trees, treeId, search, onLoadTree }: PropsT) => {
  const tree = trees[treeId];

  if (!isNodeInSearchResult(treeId, tree.children, search)) return null;

  const rootConcept = getConceptById(treeId);

  const commonProps = {
    treeId,
    search,
    onLoadTree,
    depth: 0
  };

  return tree.detailsAvailable ? (
    <ConceptTree
      id={treeId}
      label={tree.label}
      description={tree.description}
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
