import styled from "@emotion/styled";
import { forwardRef } from "react";

import FaIcon, { IconStyleProps, FaIconPropsT } from "../icon/FaIcon";

import BasicButton, { BasicButtonProps } from "./BasicButton";

interface StyledFaIconProps extends FaIconPropsT {
  tight?: boolean;
  red?: boolean;
  hasChildren: boolean;
}

const SxFaIcon = styled(FaIcon)<StyledFaIconProps>`
  color: ${({ theme, active, red }) =>
    red ? theme.col.red : active ? theme.col.blueGrayDark : theme.col.black};
  font-size: ${({ theme, large, small }) =>
    large ? theme.font.md : small ? theme.font.xs : theme.font.sm};
  margin-right: ${({ hasChildren, tight }) =>
    hasChildren ? (tight ? "5px" : "10px") : "0"};
`;

const SxBasicButton = styled(BasicButton)<{
  frame?: boolean;
  active?: boolean;
}>`
  background-color: transparent;
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.black};
  opacity: 0.7;
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

export interface IconButtonPropsT extends BasicButtonProps {
  iconProps?: IconStyleProps;
  active?: boolean;
  large?: boolean;
  small?: boolean;
  icon: string;
  regular?: boolean;
  tight?: boolean;
  red?: boolean;
  left?: boolean;
  frame?: boolean;
  bare?: boolean;
}

// A button that is prefixed by an icon
const IconButton = forwardRef<HTMLButtonElement, IconButtonPropsT>(
  (
    {
      icon,
      active,
      red,
      large,
      regular,
      left,
      children,
      tight,
      iconProps,
      small,
      ...restProps
    },
    ref,
  ) => (
    <SxBasicButton active={active} {...restProps} ref={ref}>
      <SxFaIcon
        main
        left={left}
        regular={regular}
        large={large}
        active={active}
        red={red}
        icon={icon}
        hasChildren={!!children}
        tight={tight}
        small={small}
        {...iconProps}
      />
      {children}
    </SxBasicButton>
  ),
);

export default IconButton;
