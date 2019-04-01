// @flow

import React from "react";

type PropsType = {
  className?: string,
  onDisplayAdditionalInfos: Function,
  onToggleAdditionalInfos: Function
};

const HoverableBase = (Component: any) =>
  class extends React.Component {
    props: PropsType;

    render() {
      return (
        <div
          className={this.props.className}
          onMouseEnter={this.props.onDisplayAdditionalInfos}
          onClick={this.props.onToggleAdditionalInfos}
        >
          <Component {...this.props} />
        </div>
      );
    }
  };

export default HoverableBase;
