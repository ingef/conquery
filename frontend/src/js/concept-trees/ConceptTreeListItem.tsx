import type { ConceptIdT } from "../api/types";

import ConceptTree from "./ConceptTree";
import ConceptTreeFolder from "./ConceptTreeFolder";
import { getConceptById } from "./globalTreeStoreHelper";
import type { SearchT, TreesT } from "./reducer";
import { isNodeInSearchResult } from "./selectors";

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

  if (!isNodeInSearchResult(conceptId, search, tree.children)) return null;

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
