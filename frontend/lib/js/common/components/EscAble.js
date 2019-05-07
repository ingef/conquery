// @flow

import React, { Component } from "react";

type PropsType = {
  children: any,
  className: ?string,
  onEscPressed: () => void
};

export default class EscAble extends Component<PropsType> {
  constructor(props: PropsType) {
    super(props);

    (this: any).boundKeyDownHandler = this._onKeyDown.bind(this);
  }

  componentDidMount() {
    document.addEventListener("keydown", (this: any).boundKeyDownHandler);
  }

  componentWillUnmount() {
    document.removeEventListener("keydown", (this: any).boundKeyDownHandler);
  }

  _onKeyDown = (e: Object) => {
    switch (e.keyCode) {
      case 27: // Esc key
        this.props.onEscPressed();
        break;
      default:
        break;
    }
  };

  render() {
    return <div className={this.props.className}>{this.props.children}</div>;
  }
}
