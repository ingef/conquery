import React from "react";
import styled from "@emotion/styled";
import clickOutside from "react-onclickoutside";

const Root = styled("div")`
  display: inline-block;
  text-align: left;
  cursor: initial;
  background-color: white;
  border: 1px solid ${({ theme }) => theme.col.blueGrayLight};
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 30px;
  margin: 0 20px;
  position: relative;
`;

type PropsType = {
  className?: string,
  chlidren?: React.Node,
  onClickOutside: () => void
};

class ModalContent extends React.Component {
  props: PropsType;

  handleClickOutside() {
    this.props.onClickOutside();
  }

  render() {
    return <Root className={this.props.className}>{this.props.children}</Root>;
  }
}

export default clickOutside(ModalContent);
