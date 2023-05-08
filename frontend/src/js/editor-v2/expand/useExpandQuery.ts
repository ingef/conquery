import { createId } from "@paralleldrive/cuid2";
import { useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useSelector } from "react-redux";

import { useGetQuery } from "../../api/api";
import {
  AndNodeT,
  DateRestrictionNodeT,
  OrNodeT,
  NegationNodeT,
  QueryConceptNodeT,
  SavedQueryNodeT,
  DateRangeT,
} from "../../api/types";
import { StateT } from "../../app/reducers";
import { DNDType } from "../../common/constants/dndTypes";
import { getConceptsByIdsWithTablesAndSelects } from "../../concept-trees/globalTreeStoreHelper";
import { TreesT } from "../../concept-trees/reducer";
import { mergeFromSavedConceptIntoNode } from "../../standard-query-editor/expandNode";
import {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../../standard-query-editor/types";
import { Tree } from "../types";
import { findNodeById } from "../util";

export const useExpandQuery = ({
  selectedNode,
  hotkey,
  enabled,
  tree,
  updateTreeNode,
}: {
  enabled: boolean;
  hotkey: string;
  selectedNode?: Tree;
  tree?: Tree;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
}) => {
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );
  const expandNode = useCallback(
    (
      queryNode:
        | AndNodeT
        | DateRestrictionNodeT
        | OrNodeT
        | NegationNodeT
        | QueryConceptNodeT
        | SavedQueryNodeT,
      config: {
        parentId?: string;
        negation?: boolean;
        dateRestriction?: DateRangeT;
      } = {},
    ): Tree => {
      switch (queryNode.type) {
        case "AND":
          if (queryNode.children.length === 1) {
            return expandNode(queryNode.children[0], config);
          }
          const andid = createId();
          return {
            id: andid,
            ...config,
            children: {
              connection: "and",
              direction: "horizontal",
              items: queryNode.children.map((child) =>
                expandNode(child, { parentId: andid }),
              ),
            },
          };
        case "OR":
          if (queryNode.children.length === 1) {
            return expandNode(queryNode.children[0], config);
          }
          const orid = createId();
          return {
            id: orid,
            ...config,
            children: {
              connection: "or",
              direction: "vertical",
              items: queryNode.children.map((child) =>
                expandNode(child, { parentId: orid }),
              ),
            },
          };
        case "NEGATION":
          return expandNode(queryNode.child, { ...config, negation: true });
        case "DATE_RESTRICTION":
          return expandNode(queryNode.child, {
            ...config,
            dateRestriction: queryNode.dateRange,
          });
        case "CONCEPT":
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

          return {
            id: createId(),
            data: dataNode,
            dates: config.dateRestriction
              ? {
                  ...config.dateRestriction,
                  ...(queryNode.excludeFromTimeAggregation
                    ? { excluded: true }
                    : {}),
                }
              : undefined,
            ...config,
          };
        case "SAVED_QUERY":
          console.log(queryNode);
          const dataQuery: DragItemQuery = {
            ...queryNode,
            query: undefined,
            dragContext: { width: 0, height: 0 },
            label: "", // TODO: DOUBLE CHECK
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
      const queryId = (findNodeById(tree, id)?.data as DragItemQuery).id;
      const query = await getQuery(queryId);
      updateTreeNode(id, (node) => {
        if (!query.query || query.query.root.type === "EXTERNAL_RESOLVED")
          return;

        const expanded = expandNode(query.query.root);

        Object.assign(node, expanded);
      });
    },
    [getQuery, expandNode, updateTreeNode, tree],
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
