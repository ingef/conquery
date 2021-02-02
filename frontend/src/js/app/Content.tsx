import React from "react";
import { useSelector } from "react-redux";
import styled from "@emotion/styled";
import { css } from "@emotion/react";
import SplitPane from "react-split-pane";

import Tooltip from "../tooltip/Tooltip";
import ActivateTooltip from "../tooltip/ActivateTooltip";

import type { TabT } from "../pane/types";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";
import DndProvider from "./DndProvider";
import { StateT } from "app-types";

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

export interface ContentPropsT {
  rightTabs: TabT[];
}

const Content = ({ rightTabs }: ContentPropsT) => {
  const displayTooltip = useSelector<StateT, boolean>(
    (state) => state.tooltip.displayTooltip
  );

  return (
    <DndProvider>
      <Root>
        <SplitPane
          split="vertical"
          allowResize={displayTooltip}
          minSize={displayTooltip ? 200 : 30}
          maxSize={600}
          defaultSize={displayTooltip ? "18%" : 30}
          className={!displayTooltip ? "SplitPane--tooltip-fixed" : ""}
        >
          {displayTooltip ? <Tooltip /> : <ActivateTooltip />}
          <SplitPane
            split="vertical"
            minSize={350}
            maxSize={-420}
            defaultSize="39%"
          >
            <LeftPane />
            <RightPane tabs={rightTabs} />
          </SplitPane>
        </SplitPane>
      </Root>
    </DndProvider>
  );
};

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default Content;
