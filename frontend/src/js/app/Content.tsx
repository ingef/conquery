import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import { History } from "../entity-history/History";
import PreviewV1 from "../preview/Preview";
import ActivateTooltip from "../tooltip/ActivateTooltip";
import Tooltip from "../tooltip/Tooltip";

import DndProvider from "./DndProvider";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import type { StateT } from "./reducers";
import Preview from "../preview-v2/Preview";

// ADDING TO react-split-pane STYLES
// Because otherwise, vertical panes don't expand properly in Safari
const reactSplitPaneSafariFix = css`
  .vertical {
    height: 100%;
  }
`;

const Root = styled("div")`
  width: 100%;
  height: 100%;
  position: relative;

  ${reactSplitPaneSafariFix};
`;

const Content = () => {
  const displayTooltip = useSelector<StateT, boolean>(
    (state) => state.tooltip.displayTooltip,
  );

  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  const isPreviewV1Open = useSelector<StateT, boolean>(
    (state) => state.previewV1.isOpen,
  );

  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );

  return (
    <DndProvider>
      <Root>
        {isHistoryOpen && <History />}
        {isPreviewOpen && <Preview />}
        {isPreviewV1Open && <PreviewV1 />}
        {/*
          react-split-pane is not compatible with react 18 types,
          TODO: Move to https://github.com/johnwalley/allotment
          @ts-ignore */}
        <SplitPane
          split="vertical"
          allowResize={displayTooltip}
          minSize={displayTooltip ? 200 : 30}
          maxSize={600}
          defaultSize={displayTooltip ? "15%" : 30}
          className={!displayTooltip ? "SplitPane--tooltip-fixed" : ""}
        >
          {displayTooltip ? <Tooltip /> : <ActivateTooltip />}
          {/*
          react-split-pane is not compatible with react 18 types,
          TODO: Move to https://github.com/johnwalley/allotment
          @ts-ignore */}
          <SplitPane
            split="vertical"
            minSize={350}
            maxSize={-300}
            defaultSize="42%"
          >
            <LeftPane />
            <RightPane />
          </SplitPane>
        </SplitPane>
      </Root>
    </DndProvider>
  );
};

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default Content;
