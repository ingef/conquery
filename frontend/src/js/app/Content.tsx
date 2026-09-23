import { useSelector } from "react-redux";
import { Group, Panel } from "react-resizable-panels";
import { ResizeHandle } from "../common/ResizeHandle";
import { History } from "../entity-history/History";
import InfoPane from "../info-pane/InfoPane";
import { useInfoPanePanel } from "../info-pane/useInfoPanePanel";
import Preview from "../preview/Preview";
import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";

const INFO_PANE_MIN = 200;
const INFO_PANE_MAX = 600;
const LEFT_PANE_MIN = 350;
const LEFT_PANE_DEFAULT = 600;
const RIGHT_PANE_MIN = 250;
const SEPARATOR_WIDTH = 1;
const MIN_WIDTH_FOR_INFO_PANE =
  INFO_PANE_MIN + LEFT_PANE_MIN + RIGHT_PANE_MIN + 2 * SEPARATOR_WIDTH;

const Content = () => {
  const infoPane = useInfoPanePanel({
    openSize: INFO_PANE_MIN,
    minGroupWidth: MIN_WIDTH_FOR_INFO_PANE,
  });

  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );

  return (
    <DndProvider>
      <div className="relative h-full w-full">
        <Group orientation="horizontal" {...infoPane.groupProps}>
          <Panel
            {...infoPane.panelProps}
            minSize={INFO_PANE_MIN}
            maxSize={INFO_PANE_MAX}
          >
            {infoPane.isOpen && <InfoPane />}
          </Panel>
          <ResizeHandle />
          <Panel minSize={LEFT_PANE_MIN} defaultSize={LEFT_PANE_DEFAULT}>
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
