// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import TransparentButton from "../button/TransparentButton";
import EscAble from "../common/components/EscAble";

import ModalContent from "./ModalContent";

type PropsType = {
  className?: string,
  children?: React.Node,
  headline?: React.Node,
  closeModal: Function,
  doneButton: boolean,
  tabIndex: number
};

const StyledEscAble = styled(EscAble)`
  position: fixed;
  z-index: 10;
  top: 0;
  left: 0;
  width: 100%;
  max-width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
`;

const Headline = styled("h3")`
  margin-top: 20px;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const StyledTransparentButton = styled(TransparentButton)`
  position: absolute;
  top: 12px;
  right: 15px;
`;

class Modal extends React.Component {
  props: PropsType;

  render() {
    return (
      <StyledEscAble
        className={this.props.className}
        onEscPressed={this.props.closeModal}
      >
        <ModalContent onClickOutside={this.props.closeModal}>
          {this.props.doneButton && (
            <StyledTransparentButton
              small
              onClick={this.props.closeModal}
              tabIndex={this.props.tabIndex || 0}
            >
              {T.translate("common.done")}
            </StyledTransparentButton>
          )}
          {this.props.headline && <Headline>{this.props.headline}</Headline>}
          {this.props.children}
        </ModalContent>
      </StyledEscAble>
    );
  }
}

export default Modal;
