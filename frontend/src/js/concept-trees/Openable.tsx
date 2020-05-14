import React from "react";

type PropsType = {
  openInitially?: boolean;
};

type OpenableStateT = {
  open: boolean;
};

const Openable = (Component: any) => {
  class EnhancedComponent extends React.Component<PropsType, OpenableStateT> {
    state: OpenableStateT;

    constructor(props: PropsType) {
      super(props);

      // Can't use class properties here anymore since upgrading to babel 7
      // and using @babel/plugin-proposal-class-properties
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
