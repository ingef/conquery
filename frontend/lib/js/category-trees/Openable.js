// @flow

import React from 'react';

type PropsType = {
  openInitially?: boolean,
};

type StateType = {
  open: boolean,
}

const Openable = (Component: any) => class extends React.Component {
  props: PropsType;
  state: StateType = {
    open: this.props.openInitially || false
  };

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
};

export default Openable;
