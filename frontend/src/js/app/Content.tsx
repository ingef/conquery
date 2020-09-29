import React from "react";
import { connect } from "react-redux";
import styled from "@emotion/styled";
import { css } from "@emotion/react";
// Also, set up the drag and drop context
import { DndProvider } from "react-dnd";
import HTML5Backend from "react-dnd-html5-backend";
import TouchBackend from "react-dnd-touch-backend";
import MultiBackend, {
  Preview,
  TouchTransition
} from "react-dnd-multi-backend";
// import HTML5toTouch from "react-dnd-multi-backend/lib/HTML5toTouch";
import SplitPane from "react-split-pane";
import { withRouter } from "react-router";

import Tooltip from "../tooltip/Tooltip";
import ActivateTooltip from "../tooltip/ActivateTooltip";

import type { TabT } from "../pane/types";
import LeftPane from "./LeftPane";
import RightPane from "./RightPane";

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

const PreviewItem = styled("div")`
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  opacity: 0.9;
  box-shadow: 0 0 15px 0 rgba(0, 0, 0, 0.2);
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.gray};
  width: ${({ width }) => `${width}px`};
  height: ${({ height }) => `${height}px`};
`;

// The mobile drag preview doesn't seem to be working at the moment
// Consider upgrading react-dnd BUT somehow try to keep IE11 compatibility
const generatePreview = (type, item, style) => {
  console.log("PREVIEW RENDERED", item.width, item.height, style);
  return <PreviewItem width={item.width} height={item.height} style={style} />;
};

type PropsType = {
  displayTooltip: boolean;
  rightTabs: TabT[];
};

const CustomHTML5toTouch = {
  backends: [
    {
      backend: HTML5Backend
    },
    {
      backend: TouchBackend,
      transition: TouchTransition,
      options: { enableMouseEvents: true }, // Note that you can call your backends with options
      preview: true,
      skipDispatchOnTransition: true
    }
  ]
};

const Content = ({ displayTooltip, rightTabs }: PropsType) => {
  return (
    <DndProvider backend={MultiBackend} options={CustomHTML5toTouch}>
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
        <Preview generator={generatePreview} />
      </Root>
    </DndProvider>
  );
};

const mapStateToProps = (state, ownProps) => ({
  displayTooltip: state.tooltip.displayTooltip
});

const ConnectedContent = connect(mapStateToProps)(Content);

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default withRouter(ConnectedContent);
