import { useSelector } from "react-redux";
import { Group, Panel } from "react-resizable-panels";
import { ResizeHandle } from "../common/ResizeHandle";
import { useCollapsiblePanel } from "../common/useCollapsiblePanel";
import { History } from "../entity-history/History";
import Preview from "../preview/Preview";
import ActivateTooltip from "../tooltip/ActivateTooltip";
import Tooltip from "../tooltip/Tooltip";
import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";

const Content = () => {
  const displayTooltip = useSelector<StateT, boolean>(
    (state) => state.tooltip.displayTooltip,
  );

  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );

  const tooltipPanelRef = useCollapsiblePanel(!displayTooltip);

  return (
    <DndProvider>
      <div className="relative h-full w-full">
        <Group orientation="horizontal">
          <Panel
            panelRef={tooltipPanelRef}
            collapsible
            collapsedSize={30}
            minSize={200}
            maxSize={600}
            defaultSize={displayTooltip ? 200 : 30}
          >
            {displayTooltip ? <Tooltip /> : <ActivateTooltip />}
          </Panel>
          <ResizeHandle disabled={!displayTooltip} />
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
