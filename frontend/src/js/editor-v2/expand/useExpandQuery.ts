import { createId } from "@paralleldrive/cuid2";
import { useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useSelector } from "react-redux";

import { useGetQuery } from "../../api/api";
import type {
  AndNodeT,
  DateRangeT,
  DateRestrictionNodeT,
  NegationNodeT,
  OrNodeT,
  QueryConceptNodeT,
  SavedQueryNodeT,
} from "../../api/types";
import type { StateT } from "../../app/reducers";
import { DNDType } from "../../common/constants/dndTypes";
import { getConceptsByIdsWithTablesAndSelects } from "../../concept-trees/globalTreeStoreHelper";
import type { TreesT } from "../../concept-trees/reducer";
import { mergeFromSavedConceptIntoNode } from "../../standard-query-editor/expandNode";
import type {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../../standard-query-editor/types";
import type { Tree } from "../types";
import { findNodeById } from "../util";

interface ExpandConfig {
  parentId?: string;
  negation?: boolean;
  dateRestriction?: DateRangeT;
}

type ExpandableNode =
  | AndNodeT
  | DateRestrictionNodeT
  | OrNodeT
  | NegationNodeT
  | QueryConceptNodeT
  | SavedQueryNodeT;

// AND / OR with a single child collapse into that child
const expandGroup = (
  children: ExpandableNode[],
  config: ExpandConfig,
  connection: "and" | "or",
  expand: (node: ExpandableNode, config: ExpandConfig) => Tree,
): Tree => {
  if (children.length === 1) {
    return expand(children[0], config);
  }
  const id = createId();
  return {
    id,
    ...config,
    children: {
      connection,
      direction: connection === "and" ? "horizontal" : "vertical",
      items: children.map((child) => expand(child, { parentId: id })),
    },
  };
};

const conceptNodeToTree = (
  queryNode: QueryConceptNodeT,
  config: ExpandConfig,
  rootConcepts: TreesT,
): Tree => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    queryNode.ids,
    { useDefaults: false },
  );
  if (!lookupResult) {
    throw new Error("Concept not found");
  }
  const { tables, selects } = mergeFromSavedConceptIntoNode(queryNode, {
    tables: lookupResult.tables,
    selects: lookupResult.selects || [],
  });
  const label = queryNode.label || lookupResult.concepts[0].label;
  const description = lookupResult.concepts[0].description;

  const dataNode: DragItemConceptTreeNode = {
    ...queryNode,
    dragContext: { width: 0, height: 0 },
    additionalInfos: lookupResult.concepts[0].additionalInfos,
    matchingEntities: lookupResult.concepts[0].matchingEntities,
    matchingEntries: lookupResult.concepts[0].matchingEntries,
    type: DNDType.CONCEPT_TREE_NODE,
    label,
    description,
    tables,
    selects,
    tree: lookupResult.root,
  };

  const dates = config.dateRestriction
    ? {
        ...config.dateRestriction,
        ...(queryNode.excludeFromTimeAggregation ? { excluded: true } : {}),
      }
    : undefined;

  return { id: createId(), data: dataNode, dates, ...config };
};

export const useExpandQuery = ({
  selectedNode,
  hotkey,
  enabled,
  tree,
  setSelectedNodeId,
  updateTreeNode,
}: {
  enabled: boolean;
  hotkey: string;
  selectedNode?: Tree;
  setSelectedNodeId: (id: Tree["id"] | undefined) => void;
  tree?: Tree;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
}) => {
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );
  const expandNode = useCallback(
    (queryNode: ExpandableNode, config: ExpandConfig = {}): Tree => {
      switch (queryNode.type) {
        case "AND":
          return expandGroup(queryNode.children, config, "and", expandNode);
        case "OR":
          return expandGroup(queryNode.children, config, "or", expandNode);
        case "NEGATION":
          return expandNode(queryNode.child, { ...config, negation: true });
        case "DATE_RESTRICTION":
          return expandNode(queryNode.child, {
            ...config,
            dateRestriction: queryNode.dateRange,
          });
        case "CONCEPT":
          return conceptNodeToTree(queryNode, config, rootConcepts);
        case "SAVED_QUERY":
          const dataQuery: DragItemQuery = {
            ...queryNode,
            query: undefined,
            dragContext: { width: 0, height: 0 },
            label: "", // TODO: Clarify why there is no label at this point.
            tags: [],
            type: DNDType.PREVIOUS_QUERY,
            id: queryNode.query,
          };
          return {
            id: queryNode.query,
            data: dataQuery,
            ...config,
          };
      }
    },
    [rootConcepts],
  );

  const getQuery = useGetQuery();
  const expandQuery = useCallback(
    async (id: string) => {
      if (!tree) return;
      const node = findNodeById(tree, id);
      if (!node) return;
      const queryId = (node.data as DragItemQuery).id;
      const query = await getQuery(queryId);

      updateTreeNode(id, (node) => {
        if (!query.query || query.query.root.type === "EXTERNAL_RESOLVED")
          return;

        const expanded = expandNode(query.query.root);
        setSelectedNodeId(expanded.id);

        Object.assign(node, expanded);
      });
    },
    [getQuery, expandNode, updateTreeNode, tree, setSelectedNodeId],
  );

  const canExpand = useMemo(() => {
    return (
      enabled &&
      selectedNode &&
      !selectedNode.children &&
      selectedNode.data?.type !== DNDType.CONCEPT_TREE_NODE &&
      selectedNode.data?.id
    );
  }, [enabled, selectedNode]);

  const onExpand = useCallback(() => {
    if (!canExpand) return;

    expandQuery(selectedNode!.id);
  }, [selectedNode, expandQuery, canExpand]);

  useHotkeys(hotkey, onExpand, [onExpand]);

  return {
    canExpand,
    onExpand,
  };
};
