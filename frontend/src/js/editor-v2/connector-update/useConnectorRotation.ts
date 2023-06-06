import { useCallback } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { ConnectionKind, Tree, TreeChildren } from "../types";

const CONNECTIONS = ["and", "or", "time"] as ConnectionKind[];

const getNextConnector = (
  children: TreeChildren,
  timebasedQueriesEnabled: boolean,
) => {
  const allowedConnectors = timebasedQueriesEnabled
    ? CONNECTIONS
    : CONNECTIONS.filter((c) => c !== "time");

  const index = allowedConnectors.indexOf(children.connection);

  const nextConnector =
    allowedConnectors[(index + 1) % allowedConnectors.length];

  if (nextConnector !== "time") {
    return {
      items: children.items,
      direction: children.direction,
      connection: nextConnector,
    };
  } else {
    return {
      items: children.items,
      direction: children.direction,
      connection: "time" as const,
      timestamps: children.items.map(() => "every" as const),
      operator: "before" as const,
    };
  }
};

export const useConnectorEditing = ({
  enabled,
  timebasedQueriesEnabled,
  hotkey,
  selectedNode,
  updateTreeNode,
}: {
  enabled: boolean;
  timebasedQueriesEnabled: boolean;
  hotkey: string;
  selectedNode: Tree | undefined;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
}) => {
  const onRotateConnector = useCallback(() => {
    if (!enabled || !selectedNode || !selectedNode.children) return;

    updateTreeNode(selectedNode.id, (node) => {
      if (!node.children) return;

      node.children = getNextConnector(node.children, timebasedQueriesEnabled);
    });
  }, [enabled, selectedNode, updateTreeNode, timebasedQueriesEnabled]);

  useHotkeys(hotkey, onRotateConnector, [onRotateConnector]);

  return {
    onRotateConnector,
  };
};
