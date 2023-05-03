import { useCallback } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { Tree } from "../types";

const CONNECTORS = ["and", "or", "before"] as const;

const getNextConnector = (connector: (typeof CONNECTORS)[number]) => {
  const index = CONNECTORS.indexOf(connector);
  return CONNECTORS[(index + 1) % CONNECTORS.length];
};

export const useConnectorEditing = ({
  enabled,
  hotkey,
  selectedNode,
  updateTreeNode,
}: {
  enabled: boolean;
  hotkey: string;
  selectedNode: Tree | undefined;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
}) => {
  const onRotateConnector = useCallback(() => {
    if (!enabled || !selectedNode || !selectedNode.children) return;

    updateTreeNode(selectedNode.id, (node) => {
      if (!node.children) return;

      node.children.connection = getNextConnector(node.children.connection);
    });
  }, [enabled, selectedNode, updateTreeNode]);

  useHotkeys(hotkey, onRotateConnector, [onRotateConnector]);

  return {
    onRotateConnector,
  };
};
