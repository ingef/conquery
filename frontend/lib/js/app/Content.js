// @flow

import React                        from 'react';
import { connect }                  from 'react-redux';
// Also, set up the drag and drop context
import { DragDropContext }          from 'react-dnd';
import HTML5Backend                 from 'react-dnd-html5-backend';
import SplitPane                    from 'react-split-pane';
import { withRouter }               from 'react-router';

import { Tooltip, ActivateTooltip } from '../tooltip';

import LeftPane                     from './LeftPane';
import RightPane                    from './RightPane';

type PropsType = {
  leftPaneActiveTab: string,
  rightPaneActiveTab: string,
  displayTooltip: boolean,
};

const Content = (props: PropsType) => {
  return (
    <div className="content">
      <SplitPane
        split="horizontal"
        primary="second"
        allowResize={props.displayTooltip}
        minSize={props.displayTooltip ? 80 : 30}
        maxSize={-400}
        defaultSize={props.displayTooltip ? "10%" : 30}
        className={!props.displayTooltip ? 'SplitPane--tooltip-fixed' : ''}
      >
        <SplitPane
          split="vertical"
          minSize={350}
          maxSize={-420}
          defaultSize="50%"
        >
          <LeftPane activeTab={props.leftPaneActiveTab} />
          <RightPane activeTab={props.rightPaneActiveTab} />
        </SplitPane>
        {
          props.displayTooltip
          ? <Tooltip />
          : <ActivateTooltip />
        }
      </SplitPane>
    </div>
  );
};

const mapStateToProps = (state, ownProps) => ({
  leftPaneActiveTab: state.panes.left.activeTab,
  rightPaneActiveTab: state.panes.right.activeTab,
  displayTooltip: state.tooltip.displayTooltip,
});

const ConnectedContent = connect(mapStateToProps)(Content);

export default withRouter(DragDropContext(HTML5Backend)(ConnectedContent));
