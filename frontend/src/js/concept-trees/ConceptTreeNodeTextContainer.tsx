import { useRef, FC } from "react";
import { useDrag } from "react-dnd";

import type { ConceptIdT, ConceptT } from "../api/types";
import { getWidthAndHeight } from "../app/DndProvider";
import { DNDType } from "../common/constants/dndTypes";
import { exists } from "../common/helpers/exists";
import { getNodeIcon } from "../model/node";
import type {
  ConceptQueryNodeType,
  DragItemConceptTreeNode,
} from "../standard-query-editor/types";
import AdditionalInfoHoverable from "../tooltip/AdditionalInfoHoverable";

import ConceptTreeNodeText from "./ConceptTreeNodeText";
import type { SearchT } from "./reducer";

interface PropsT {
  conceptId: ConceptIdT;
  node: ConceptT;
  root: ConceptT;
  open: boolean;
  depth: number;
  active?: boolean;
  onTextClick?: () => void;
  createQueryElement?: () => ConceptQueryNodeType;
  search: SearchT;
  isStructFolder?: boolean;
}

function getResultCount(
  search: SearchT,
  node: ConceptT,
  conceptId: ConceptIdT,
) {
  if (!search.result) {
    return null;
  }

  return search.result[conceptId] > 0 &&
    node.children &&
    node.children.some((child) => search.result && search.result[child] > 0)
    ? search.result[conceptId]
    : null;
}

const ConceptTreeNodeTextContainer: FC<PropsT> = ({
  conceptId,
  node,
  root,
  depth,
  search,
  active,
  open,
  onTextClick,
  isStructFolder,
  createQueryElement,
}) => {
  const ref = useRef<HTMLDivElement | null>(null);

  const red = exists(node.matchingEntries) && node.matchingEntries === 0;
  const resultCount = getResultCount(search, node, conceptId);
  const hasChildren = !!node.children && node.children.length > 0;

  const item: DragItemConceptTreeNode = {
    dragContext: {
      height: 0,
      width: 0,
    },
    type: DNDType.CONCEPT_TREE_NODE,
    ...(createQueryElement
      ? createQueryElement() // Should always be defined when draggable => when active === true
      : {
          ids: [],
          tables: [],
          selects: [],
          tree: conceptId,
          label: "",
          matchingEntities: 0,
          matchingEntries: 0,
        }),
  };
  const [, drag] = useDrag<DragItemConceptTreeNode, void>({
    type: item.type,
    item: () => ({
      ...item,
      dragContext: {
        ...item.dragContext,
        ...getWidthAndHeight(ref),
      },
    }),
  });

  const icon = getNodeIcon(node, {
    isStructNode: isStructFolder || active === false,
    open,
  });

  return (
    <AdditionalInfoHoverable node={node} root={root}>
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
        icon={icon}
        red={red}
        onClick={onTextClick}
      />
    </AdditionalInfoHoverable>
  );
};

export default ConceptTreeNodeTextContainer;
