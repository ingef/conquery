import React, { useRef, FC } from "react";
import { useDrag } from "react-dnd";

import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";
import { isEmpty } from "../common/helpers";
import { CONCEPT_TREE_NODE } from "../common/constants/dndTypes";

import type { AdditionalInfoHoverableNodeType } from "../tooltip/AdditionalInfoHoverable";
import type { DraggedNodeType } from "../standard-query-editor/types";
import type { SearchT } from "./reducer";

import ConceptTreeNodeText from "./ConceptTreeNodeText";
import { getWidthAndHeight } from "../app/DndProvider";

interface PropsT {
  node: AdditionalInfoHoverableNodeType & {
    label: string;
    description?: string;
    matchingEntries?: number;
  };
  open: boolean;
  depth: number;
  active?: boolean;
  onTextClick?: Function;
  createQueryElement: () => DraggedNodeType;
  search?: SearchT;
  isStructFolder?: boolean;
}

function getResultCount(search, node) {
  return search.result &&
    search.result[node.id] > 0 &&
    node.children &&
    node.children.some((child) => search.result[child] > 0)
    ? search.result[node.id]
    : null;
}

const ConceptTreeNodeTextContainer: FC<PropsT> = ({
  node,
  depth,
  search,
  active,
  open,
  onTextClick,
  isStructFolder,
  createQueryElement,
}) => {
  const ref = useRef<HTMLDivElement | null>(null);

  const red = !isEmpty(node.matchingEntries) && node.matchingEntries === 0;
  const resultCount = getResultCount(search, node);
  const hasChildren = !!node.children && node.children.length > 0;

  const item = {
    height: 0,
    width: 0,
    type: CONCEPT_TREE_NODE,
    ...createQueryElement(),
  };
  const [, drag] = useDrag({
    item,
    begin: () => ({
      ...item,
      ...getWidthAndHeight(ref),
    }),
  });

  return (
    <ConceptTreeNodeText
      ref={(instance) => {
        ref.current = instance;

        // Don't allow dragging with inactive elements
        if (active !== false) {
          drag(instance);
        }
      }}
      label={node.label}
      depth={depth}
      description={node.description}
      resultCount={resultCount}
      searchWords={search.words}
      hasChildren={hasChildren}
      isOpen={open}
      isStructFolder={isStructFolder || active === false}
      red={red}
      onClick={onTextClick}
    />
  );
};

export default AdditionalInfoHoverable(ConceptTreeNodeTextContainer);
