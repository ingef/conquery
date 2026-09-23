import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import type {
  GroupProps,
  LayoutChangedMeta,
  PanelProps,
  PanelSize,
} from "react-resizable-panels";
import type { StateT } from "../app/reducers";
import { useCollapsiblePanel } from "../common/useCollapsiblePanel";
import { collapseInfoPane, expandInfoPane } from "./actions";

/**
 * Keeps the info pane's Panel and its state in sync. The library collapses the
 * panel on a drag under its minimum or in a too narrow window; the state records
 * which, so only a window-closed pane reopens once the group fits it again.
 */
export const useInfoPanePanel = ({
  openSize,
  minGroupWidth,
}: {
  openSize: number;
  /** the group width at which every pane has room for its minimum */
  minGroupWidth: number;
}) => {
  const isOpen = useSelector<StateT, boolean>((state) => state.infoPane.isOpen);
  const collapsedByLayout = useSelector<StateT, boolean>(
    (state) => state.infoPane.collapsedByLayout,
  );
  const dispatch = useDispatch();

  const panelRef = useCollapsiblePanel(!isOpen);
  const groupRef = useRef<HTMLDivElement>(null);
  // read once, a changing default resets the group's layout
  const [defaultSize] = useState(() => (isOpen ? openSize : 0));

  const onLayoutChanged = (
    _: unknown,
    { isUserInteraction }: LayoutChangedMeta,
  ) => {
    if (isOpen && panelRef.current?.isCollapsed()) {
      dispatch(collapseInfoPane({ byLayout: !isUserInteraction }));
    }
  };

  // dragged open from the collapsed edge; live, so the content shows during the drag
  const onResize = ({ inPixels }: PanelSize) => {
    if (inPixels > 0 && !isOpen) dispatch(expandInfoPane());
  };

  useEffect(() => {
    const group = groupRef.current;
    if (!collapsedByLayout || !group) return;

    const observer = new ResizeObserver(([entry]) => {
      if (entry.contentRect.width >= minGroupWidth) dispatch(expandInfoPane());
    });
    observer.observe(group);
    return () => observer.disconnect();
  }, [collapsedByLayout, minGroupWidth, dispatch]);

  return {
    isOpen,
    groupProps: {
      elementRef: groupRef,
      onLayoutChanged,
    } satisfies GroupProps,
    panelProps: {
      panelRef,
      collapsible: true,
      collapsedSize: 0,
      defaultSize,
      onResize,
    } satisfies PanelProps,
  };
};
