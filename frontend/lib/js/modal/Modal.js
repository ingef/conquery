// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import IconButton from "../button/IconButton";
import TransparentButton from "../button/TransparentButton";

import EscAble from "../common/components/EscAble";

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

  closeMaybe = (e: Object) => {
    if (
      typeof e.nativeEvent.target.className === "string" && // When SVG Fa Icon is clicked
      e.nativeEvent.target.className.indexOf("modal__center") !== -1
    )
      this.props.closeModal();
  };

  render() {
    return (
      <EscAble
        className="modal"
        onClick={this.closeMaybe}
        onEscPressed={this.props.closeModal}
      >
        <div className="modal__center">
          <div className="modal__content">
            {this.props.doneButton ? (
              <StyledTransparentButton
                small
                onClick={this.props.closeModal}
                tabIndex={this.props.tabIndex || 0}
              >
                {T.translate("common.done")}
              </StyledTransparentButton>
            ) : (
              <StyledIconButton icon="close" onClick={this.props.closeModal} />
            )}
            {this.props.children}
          </div>
        </div>
      </EscAble>
    );
  }
}

export default Modal;
