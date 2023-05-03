import { useState, useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { Tree } from "../types";

export const useDateEditing = ({
  enabled,
  hotkey,
  selectedNode,
}: {
  enabled: boolean;
  hotkey: string;
  selectedNode: Tree | undefined;
}) => {
  const [showModal, setShowModal] = useState(false);

  const onClose = useCallback(() => setShowModal(false), []);
  const onOpen = useCallback(() => {
    if (!enabled) return;
    if (!selectedNode) return;

    setShowModal(true);
  }, [enabled, selectedNode]);

  useHotkeys(hotkey, onOpen, [onOpen], {
    preventDefault: true,
  });

  const headline = useMemo(() => {
    if (!selectedNode) return "";

    return (
      selectedNode.data?.label ||
      (selectedNode.children?.items || []).map((c) => c.data?.label).join(" ")
    );
  }, [selectedNode]);

  return {
    showModal,
    headline,
    onClose,
    onOpen,
  };
};
