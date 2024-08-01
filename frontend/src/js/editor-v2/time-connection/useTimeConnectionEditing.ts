import { useCallback, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import { Tree } from "../types";

export const useTimeConnectionEditing = ({
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

  return {
    showModal,
    onClose,
    onOpen,
  };
};
