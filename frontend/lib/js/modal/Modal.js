// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import IconButton from "../button/IconButton";
import TransparentButton from "../button/TransparentButton";

type PropsType = {
  children?: Element[],
  closeModal: Function,
  doneButton: boolean,
  tabIndex: number
};

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 12px;
  right: 15px;
`;
const StyledTransparentButton = styled(TransparentButton)`
  position: absolute;
  top: 12px;
  right: 15px;
`;

class Modal extends React.Component {
  props: PropsType;

  constructor(props: PropsType) {
    super(props);

    (this: any).boundKeyUpHandler = this._onKeyUp.bind(this);
  }

  componentDidMount() {
    window.addEventListener("keyup", (this: any).boundKeyUpHandler);
  }

  componentWillUnmount() {
    window.removeEventListener("keyup", (this: any).boundKeyUpHandler);
  }

  _onKeyUp(e: Object) {
    switch (e.keyCode) {
      case 27: // Esc key
        this.props.closeModal();
        break;
      default:
        break;
    }
  }

  _closeMaybe(e: Object) {
    if (e.nativeEvent.target.className.indexOf("modal__center") !== -1)
      this.props.closeModal();
  }

  render() {
    const closeButton = this.props.doneButton ? (
      <StyledTransparentButton
        small
        onClick={this.props.closeModal}
        tabIndex={this.props.tabIndex || 0}
      >
        {T.translate("common.done")}
      </StyledTransparentButton>
    ) : (
      <StyledIconButton icon="close" onClick={this.props.closeModal} />
    );

    return (
      <div className="modal" onClick={this._closeMaybe.bind(this)}>
        <div className="modal__center">
          <div className="modal__content">
            {closeButton}
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}

export default Modal;
