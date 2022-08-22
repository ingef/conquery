import styled from "@emotion/styled";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { forwardRef, memo } from "react";

import FaIcon, { IconStyleProps, FaIconPropsT } from "../icon/FaIcon";

import BasicButton, { BasicButtonProps } from "./BasicButton";

interface StyledFaIconProps extends FaIconPropsT {
  tight?: boolean;
  red?: boolean;
  secondary?: boolean;
  hasChildren: boolean;
}

const SxFaIcon = styled(FaIcon)<StyledFaIconProps>`
  color: ${({ theme, active, red, secondary, light }) =>
    red
      ? theme.col.red
      : active
      ? theme.col.blueGrayDark
      : light
      ? theme.col.gray
      : secondary
      ? theme.col.orange
      : theme.col.black};
  font-size: ${({ theme, large, small }) =>
    large ? theme.font.md : small ? theme.font.xs : theme.font.sm};
  margin-right: ${({ hasChildren, tight }) =>
    hasChildren ? (tight ? "5px" : "10px") : "0"};
`;

const SxBasicButton = styled(BasicButton)<{
  frame?: boolean;
  active?: boolean;
  secondary?: boolean;
}>`
  background-color: transparent;
  color: ${({ theme, active, secondary }) =>
    active
      ? theme.col.blueGrayDark
      : secondary
      ? theme.col.orange
      : theme.col.black};
  opacity: 0.75;
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
  icon: IconName;
  regular?: boolean;
  secondary?: boolean;
  tight?: boolean;
  red?: boolean;
  left?: boolean;
  frame?: boolean;
  bare?: boolean;
  light?: boolean;
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
      secondary,
      light,
      ...restProps
    },
    ref,
  ) => {
    return (
      <SxBasicButton
        active={active}
        secondary={secondary}
        {...restProps}
        ref={ref}
      >
        <SxFaIcon
          main
          left={left}
          regular={regular}
          large={large}
          active={active}
          red={red}
          secondary={secondary}
          icon={icon}
          hasChildren={!!children}
          tight={tight}
          small={small}
          light={light}
          {...iconProps}
        />
        {children}
      </SxBasicButton>
    );
  },
);

export default memo(IconButton);
