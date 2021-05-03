import styled from "@emotion/styled";
import * as React from "react";

import FaIcon, { IconStyleProps, FaIconPropsT } from "../icon/FaIcon";

import BasicButton from "./BasicButton";

interface StyledFaIconProps extends FaIconPropsT {
  tight?: boolean;
  red?: boolean;
  hasChildren: boolean;
}

const StyledFaIcon = styled(FaIcon)<StyledFaIconProps>`
  color: ${({ theme, active, red }) =>
    red ? theme.col.red : active ? theme.col.blueGrayDark : theme.col.black};
  font-size: ${({ theme, large }) => (large ? theme.font.lg : theme.font.sm)};
  margin-right: ${({ hasChildren, tight }) =>
    hasChildren ? (tight ? "5px" : "10px") : "0"};
`;

const StyledTransparentButton = styled(BasicButton)<{ frame?: boolean }>`
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
      opacity: 0.6;
    }
  }
`;

export interface IconButtonPropsT
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  iconProps?: IconStyleProps;
  active?: boolean;
  large?: boolean;
  icon: string;
  regular?: boolean;
  tight?: boolean;
  red?: boolean;
  left?: boolean;
  frame?: boolean;
  bare?: boolean;
  onClick: React.ButtonHTMLAttributes<HTMLButtonElement>["onClick"];
}

// A button that is prefixed by an icon
const IconButton: React.FC<IconButtonPropsT> = ({
  icon,
  active,
  red,
  large,
  regular,
  left,
  children,
  tight,
  iconProps,
  ...restProps
}) => (
  <StyledTransparentButton active={active} {...restProps}>
    <StyledFaIcon
      main
      left={left}
      regular={regular}
      large={large}
      active={active}
      red={red}
      icon={icon}
      hasChildren={!!children}
      tight={tight}
      {...iconProps}
    />
    {children}
  </StyledTransparentButton>
);

export default IconButton;
