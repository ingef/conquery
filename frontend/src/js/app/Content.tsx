import { useState } from "react";
import { useSelector } from "react-redux";
import { Group, Panel } from "react-resizable-panels";
import { ResizeHandle } from "../common/ResizeHandle";
import { useCollapsiblePanel } from "../common/useCollapsiblePanel";
import { History } from "../entity-history/History";
import InfoPane from "../info-pane/InfoPane";
import InfoPaneCollapsed from "../info-pane/InfoPaneCollapsed";
import Preview from "../preview/Preview";
import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";

const Content = () => {
  const isInfoPaneOpen = useSelector<StateT, boolean>(
    (state) => state.infoPane.isOpen,
  );

  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );

  const infoPaneRef = useCollapsiblePanel(!isInfoPaneOpen);
  // read once, a changing default resets the group's layout
  const [infoPaneDefaultSize] = useState(() => (isInfoPaneOpen ? 200 : 30));

  return (
    <DndProvider>
      <div className="relative h-full w-full">
        <Group orientation="horizontal">
          <Panel
            panelRef={infoPaneRef}
            collapsible
            collapsedSize={30}
            minSize={200}
            maxSize={600}
            defaultSize={infoPaneDefaultSize}
          >
            {isInfoPaneOpen ? <InfoPane /> : <InfoPaneCollapsed />}
          </Panel>
          <ResizeHandle disabled={!isInfoPaneOpen} />
          <Panel minSize={350} defaultSize={600}>
            <LeftPane />
          </Panel>
          <ResizeHandle />
          <Panel minSize={250}>
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
