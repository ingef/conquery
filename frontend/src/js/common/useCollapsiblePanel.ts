import { useEffect, useRef } from "react";
import { usePanelRef } from "react-resizable-panels";

// react-resizable-panels only exposes collapse/expand imperatively,
// this keeps a `collapsible` Panel in sync with a boolean from state.
// a panel that starts collapsed expands to `initialSize` first, then to its last size
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
