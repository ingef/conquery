// @flow

import * as React from "react";
import styled from "@emotion/styled";
import BasicButton from "./BasicButton";
import FaIcon from "../icon/FaIcon";

type PropsType = {
  children?: React.Node,
  iconProps?: Object,
  active?: boolean,
  large?: boolean,
  icon: string
};

const StyledFaIcon = styled(FaIcon)`
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.black};
`;

const StyledTransparentButton = styled(BasicButton)`
  background-color: transparent;
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.black};
  opacity: 0.8;
  transition: opacity ${({ theme }) => theme.transitionTime};

  border-radius: 2px;
  border: ${({ theme, frame }) =>
    frame ? "1px solid " + theme.col.gray : "none"};

  &:hover {
    opacity: 1;
  }

  &:disabled {
    &:hover {
      opacity: 0.8;
    }
  }
`;

// A button that is prefixed by an icon
const IconButton = ({
  icon,
  active,
  large,
  children,
  iconProps,
  ...restProps
}: PropsType) => (
  <StyledTransparentButton small={!large} active={active} {...restProps}>
    <StyledFaIcon main active={active} icon={icon} {...iconProps} /> {children}
  </StyledTransparentButton>
);

export default IconButton;
