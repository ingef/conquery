import { useCallback } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { Tree } from "../types";

export const useNegationEditing = ({
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
  const onNegateClick = useCallback(() => {
    if (!selectedNode || !enabled) return;

    updateTreeNode(selectedNode.id, (node) => {
      node.negation = !node.negation;
    });
  }, [enabled, selectedNode, updateTreeNode]);

  useHotkeys(hotkey, onNegateClick, [onNegateClick]);

  return {
    onNegateClick,
  };
};
