// @flow

import React from "react";
import { connect } from "react-redux";
// Also, set up the drag and drop context
import { DragDropContext } from "react-dnd";
import HTML5Backend from "react-dnd-html5-backend";
import SplitPane from "react-split-pane";
import { withRouter } from "react-router";

import { Tooltip, ActivateTooltip } from "../tooltip";

import LeftPane from "./LeftPane";
import RightPane from "./RightPane";

type PropsType = {
  displayTooltip: boolean
};

const Content = ({ displayTooltip }: PropsType) => {
  return (
    <div className="content">
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
    </div>
  );
};

const mapStateToProps = (state, ownProps) => ({
  displayTooltip: state.tooltip.displayTooltip
});

const ConnectedContent = connect(mapStateToProps)(Content);

export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
