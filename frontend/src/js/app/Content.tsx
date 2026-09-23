import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  Group,
  type LayoutChangedMeta,
  Panel,
  type PanelSize,
} from "react-resizable-panels";
import { ResizeHandle } from "../common/ResizeHandle";
import { useCollapsiblePanel } from "../common/useCollapsiblePanel";
import { History } from "../entity-history/History";
import { collapseInfoPane, expandInfoPane } from "../info-pane/actions";
import InfoPane from "../info-pane/InfoPane";
import Preview from "../preview/Preview";
import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";

const INFO_PANE_MIN = 200;
const INFO_PANE_MAX = 600;
const LEFT_PANE_MIN = 350;
const RIGHT_PANE_MIN = 250;
const SEPARATOR_WIDTH = 1;

const fitsInfoPane = (groupWidth: number) =>
  groupWidth >=
  INFO_PANE_MIN + LEFT_PANE_MIN + RIGHT_PANE_MIN + 2 * SEPARATOR_WIDTH;

const Content = () => {
  const isInfoPaneOpen = useSelector<StateT, boolean>(
    (state) => state.infoPane.isOpen,
  );
  const isInfoPaneCollapsedByLayout = useSelector<StateT, boolean>(
    (state) => state.infoPane.collapsedByLayout,
  );
  const dispatch = useDispatch();

  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );

  const infoPaneRef = useCollapsiblePanel(!isInfoPaneOpen);
  const groupRef = useRef<HTMLDivElement>(null);
  // read once, a changing default resets the group's layout
  const [infoPaneDefaultSize] = useState(() =>
    isInfoPaneOpen ? INFO_PANE_MIN : 0,
  );

  // the library collapses the panel on a drag under its minimum or in a too
  // narrow window; mirror that into the state
  const onLayoutChanged = (
    _: unknown,
    { isUserInteraction }: LayoutChangedMeta,
  ) => {
    if (isInfoPaneOpen && infoPaneRef.current?.isCollapsed()) {
      dispatch(collapseInfoPane({ byLayout: !isUserInteraction }));
    }
  };

  // dragged open from the collapsed edge; live, so the content shows during the drag
  const onInfoPaneResize = ({ inPixels }: PanelSize) => {
    if (inPixels > 0 && !isInfoPaneOpen) dispatch(expandInfoPane());
  };

  // reopen a panel that only the window closed, as soon as the window fits it again
  useEffect(() => {
    const group = groupRef.current;
    if (!isInfoPaneCollapsedByLayout || !group) return;

    const observer = new ResizeObserver(([entry]) => {
      if (fitsInfoPane(entry.contentRect.width)) dispatch(expandInfoPane());
    });
    observer.observe(group);
    return () => observer.disconnect();
  }, [isInfoPaneCollapsedByLayout, dispatch]);

  return (
    <DndProvider>
      <div className="relative h-full w-full">
        <Group
          orientation="horizontal"
          elementRef={groupRef}
          onLayoutChanged={onLayoutChanged}
        >
          <Panel
            panelRef={infoPaneRef}
            collapsible
            collapsedSize={0}
            minSize={INFO_PANE_MIN}
            maxSize={INFO_PANE_MAX}
            defaultSize={infoPaneDefaultSize}
            onResize={onInfoPaneResize}
          >
            {isInfoPaneOpen && <InfoPane />}
          </Panel>
          <ResizeHandle />
          <Panel minSize={LEFT_PANE_MIN} defaultSize={600}>
            <LeftPane />
          </Panel>
          <ResizeHandle />
          <Panel minSize={RIGHT_PANE_MIN}>
            <RightPane />
          </Panel>
        </Group>
        {isHistoryOpen && <History />}
        {isPreviewOpen && <Preview />}
      </div>
    </DndProvider>
  );
};

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default Content;
