// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import onClickOutside from "react-onclickoutside";

import useEscPress from "../hooks/useEscPress";

import TransparentButton from "../button/TransparentButton";

const Root = styled("div")`
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

const Content = styled("div")`
  display: inline-block;
  text-align: left;
  cursor: initial;
  background-color: white;
  box-shadow: 0 0 15px 0 rgba(0, 0, 0, 0.2);
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 30px;
  margin: 0 20px;
  position: relative;
`;

const Headline = styled("h3")`
  margin-top: 20px;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const SxButton = styled(TransparentButton)`
  position: absolute;
  top: 12px;
  right: 15px;
`;

// https://github.com/Pomax/react-onclickoutside
type ContentPropsT = {
  children?: React.Node,
  onClose: () => void
};

const ModalContentComponent = ({ children, onClose }: ContentPropsT) => {
  ModalContentComponent.handleClickOutside = onClose;

  return <Content>{children}</Content>;
};

const ModalContent = onClickOutside(ModalContentComponent, {
  handleClickOutside: () => ModalContentComponent.handleClickOutside
});
// -----------------------------------------------

type PropsT = {
  children?: React.Node,
  className?: string,
  headline?: React.Node,
  doneButton?: boolean,
  tabIndex: number,
  onClose: () => void
};

// A modal with three ways to close it
// - a button
// - click outside
// - press esc
const Modal = ({
  className,
  children,
  headline,
  tabIndex,
  doneButton,
  onClose
}: PropsT) => {
  useEscPress(onClose);

  return (
    <Root className={className}>
      <ModalContent onClose={onClose}>
        {doneButton && (
          <SxButton
            small
            tabIndex={tabIndex || 0}
            icon="times"
            onClick={onClose}
          >
            {T.translate("common.done")}
          </SxButton>
        )}
        {headline && <Headline>{headline}</Headline>}
        {children}
      </ModalContent>
    </Root>
  );
};

export default Modal;
