// @flow

import React from 'react';

type PropsType = {
  onDisplayAdditionalInfos: Function,
  onHideAdditionalInfos: Function,
};

const HoverableBase = (Component: any) => class extends React.Component {
  props: PropsType;

  render() {
    return (
      <div
        className="additional-info-hoverable"
        onMouseEnter={this.props.onDisplayAdditionalInfos}
        onMouseLeave={this.props.onHideAdditionalInfos}
      >
        <Component {...this.props} />
      </div>
    );
  }
};

export default HoverableBase;
