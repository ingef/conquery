// @flow

import React from "react";
import { connect } from "react-redux";
import styled from "@emotion/styled";
// Also, set up the drag and drop context
import { DragDropContext } from "react-dnd";
// import HTML5Backend from "react-dnd-html5-backend";
import MultiBackend, { Preview } from "react-dnd-multi-backend";
import HTML5toTouch from "react-dnd-multi-backend/lib/HTML5toTouch";
import SplitPane from "react-split-pane";
import { withRouter } from "react-router";

import { Tooltip, ActivateTooltip } from "../tooltip";

import LeftPane from "./LeftPane";
import RightPane from "./RightPane";

const Root = styled("div")`
  width: 100%;
  height: 100%;
  position: relative;

  // ADDING TO react-split-pane STYLES
  // Because otherwise, vertical panes don't expand properly in Safari
  .vertical {
    height: 100%;
  }
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

const generatePreview = (type, item, style) => {
  return <PreviewItem width={item.width} height={item.height} style={style} />;
};

type PropsType = {
  displayTooltip: boolean
};

const Content = ({ displayTooltip }: PropsType) => {
  return (
    <Root>
      <SplitPane
        split="horizontal"
        primary="second"
        allowResize={displayTooltip}
        minSize={displayTooltip ? 80 : 30}
        maxSize={-400}
        defaultSize={displayTooltip ? "10%" : 30}
        className={!displayTooltip ? "SplitPane--tooltip-fixed" : ""}
      >
        <SplitPane
          split="vertical"
          minSize={350}
          maxSize={-420}
          defaultSize="50%"
        >
          <LeftPane />
          <RightPane />
        </SplitPane>
        {displayTooltip ? <Tooltip /> : <ActivateTooltip />}
      </SplitPane>
      <Preview generator={generatePreview} />
    </Root>
  );
};

const mapStateToProps = (state, ownProps) => ({
  displayTooltip: state.tooltip.displayTooltip
});

const ConnectedContent = connect(mapStateToProps)(Content);

// export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
export default withRouter(
  DragDropContext(MultiBackend(HTML5toTouch))(ConnectedContent)
);
