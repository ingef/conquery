import * as React from "react";
import styled from "@emotion/styled";
import BasicButton from "./BasicButton";
import FaIcon from "../icon/FaIcon";

type PropsType = {
  children?: React.Node;
  iconProps?: Object;
  active?: boolean;
  large?: boolean;
  icon: string;
};

const StyledFaIcon = styled(FaIcon)`
  color: ${({ theme, active, red }) =>
    red ? theme.col.red : active ? theme.col.blueGrayDark : theme.col.black};
  font-size: ${({ theme, large }) => (large ? theme.font.lg : theme.font.sm)};
  margin-right: ${({ hasChildren, tight }) =>
    hasChildren ? (tight ? "5px" : "10px") : "0"};
`;

const StyledTransparentButton = styled(BasicButton)`
  background-color: transparent;
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.black};
  opacity: 0.8;
  transition: opacity ${({ theme }) => theme.transitionTime};

  border-radius: ${({ theme }) => theme.borderRadius};
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
  red,
  large,
  regular,
  tight,
  left,
  children,
  iconProps,
  ...restProps
}: PropsType) => (
  <StyledTransparentButton active={active} {...restProps}>
    <StyledFaIcon
      main
      left={left}
      hasChildren={!!children}
      regular={regular}
      large={large}
      active={active}
      tight={tight}
      red={red}
      icon={icon}
      {...iconProps}
    />
    {children}
  </StyledTransparentButton>
);

export default IconButton;
