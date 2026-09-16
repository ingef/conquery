import { useEffect, useRef } from "react";
import { usePanelRef } from "react-resizable-panels";

// react-resizable-panels only exposes collapse/expand imperatively,
// this keeps a `collapsible` Panel in sync with a boolean from state.
// A panel that starts collapsed has no size to expand back to, so its first
// expansion gets `initialSize` when given; later ones restore the most recent
// size.
export const useCollapsiblePanel = (
  collapsed: boolean,
  initialSize?: number | string,
) => {
  const panelRef = usePanelRef();
  const wasExpanded = useRef(!collapsed);

  useEffect(() => {
    if (collapsed) {
      panelRef.current?.collapse();
      return;
    }

    panelRef.current?.expand();
    if (!wasExpanded.current && initialSize !== undefined) {
      panelRef.current?.resize(initialSize);
    }
    wasExpanded.current = true;
  }, [collapsed, initialSize, panelRef]);

  return panelRef;
};
