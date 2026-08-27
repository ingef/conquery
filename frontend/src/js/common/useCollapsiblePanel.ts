import { useEffect } from "react";
import { usePanelRef } from "react-resizable-panels";

// react-resizable-panels only exposes collapse/expand imperatively,
// this keeps a `collapsible` Panel in sync with a boolean from state.
export const useCollapsiblePanel = (collapsed: boolean) => {
  const panelRef = usePanelRef();

  useEffect(() => {
    if (collapsed) {
      panelRef.current?.collapse();
    } else {
      panelRef.current?.expand();
    }
  }, [collapsed, panelRef]);

  return panelRef;
};
