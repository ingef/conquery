// @flow

import React from 'react';

type PropsType = {
  openInitially?: boolean,
};

type StateType = {
  open: boolean,
}

const Openable = (Component: any) => {
  class EnhancedComponent extends React.Component<PropsType, StateType> {
    // Can't use class properties here anymore since @babel/plugin-proposal-class-properties
    constructor(props) {
      super(props);

      this.state = {
        open: props.openInitially || false
      };
    }

    _toggleNode() {
      this.setState({ open: !this.state.open });
    }

    render() {
      return (
        <Component
          {...this.props}
          open={this.state.open}
          onToggleOpen={this._toggleNode.bind(this)}
        />
      );
    }
  }

  return EnhancedComponent;
};

export default Openable;
