// @flow

import * as React from "react";
import styled from "@emotion/styled";
import BasicButton from "./BasicButton";
import FaIcon, { Icon } from "../icon/FaIcon";

type PropsType = {
  children?: React.Node,
  iconProps?: Object,
  active?: boolean,
  large?: boolean,
  icon: string
};

const StyledTransparentButton = styled(BasicButton)`
  background-color: transparent;
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.blueGray};

  border-radius: 2px;
  border: ${({ theme, frame }) =>
    frame ? "1px solid " + theme.col.gray : "none"};

  &:hover {
    color: ${({ theme, active }) =>
      active ? theme.col.blueGray : theme.col.blueGrayDark};

    ${Icon} {
      color: ${({ theme, active }) =>
        active ? theme.col.blueGray : theme.col.blueGrayDark};
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
    <FaIcon main active={active} icon={icon} {...iconProps} /> {children}
  </StyledTransparentButton>
);

export default IconButton;
