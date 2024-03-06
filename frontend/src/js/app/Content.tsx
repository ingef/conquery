import styled from "@emotion/styled";
import { useSelector } from "react-redux";

import { History } from "../entity-history/History";
import ActivateTooltip from "../tooltip/ActivateTooltip";
import Tooltip from "../tooltip/Tooltip";

import { useMemo } from "react";
import { Panel, PanelGroup } from "react-resizable-panels";
import { ResizeHandle } from "../common/ResizeHandle";
import Preview from "../preview/Preview";
import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";

const Root = styled("div")`
  width: 100%;
  height: 100%;
  position: relative;
`;

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

  const disableDragHandles = useSelector<StateT, boolean>(
    (state) => state.panes.disableDragHandles,
  );

  const collapsedStyles = useMemo(() => {
    if (displayTooltip) return {};

    return {
      width: "30px",
      minWidth: "30px",
      maxWidth: "30px",
      overflow: "hidden",
    };
  }, [displayTooltip]);

  return (
    <DndProvider>
      <Root>
        {isHistoryOpen && <History />}
        {isPreviewOpen && <Preview />}
        <PanelGroup direction="horizontal" units="pixels">
          <Panel
            style={collapsedStyles}
            minSize={200}
            maxSize={600}
            defaultSize={displayTooltip ? 200 : 30}
          >
            {displayTooltip ? <Tooltip /> : <ActivateTooltip />}
          </Panel>
          {!disableDragHandles && <ResizeHandle />}
          <Panel minSize={350} defaultSize={600}>
            <LeftPane />
          </Panel>
          {!disableDragHandles && <ResizeHandle />}
          <Panel minSize={250}>
            <RightPane />
          </Panel>
        </PanelGroup>
      </Root>
    </DndProvider>
  );
};

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default Content;
