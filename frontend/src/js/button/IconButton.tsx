import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { memo, type Ref, useMemo } from "react";

import FaIcon, { type IconStyleProps } from "../icon/FaIcon";
import { tv } from "../tv";

import BasicButton, { type BasicButtonProps } from "./BasicButton";

const iconButton = tv({
  base: [
    "inline-flex items-center",
    "gap-[10px]",
    "rounded",
    "bg-transparent",
    "text-sm",
    "text-gray-800",
    "opacity-75 hover:opacity-100 disabled:hover:opacity-40",
    "transition-[opacity,background-color] duration-100",
  ],
  variants: {
    // later wins when several are set
    secondary: { true: "text-orange" },
    active: { true: "text-primary-500" },
    red: { true: "text-red" },
    frame: { true: "opacity-100 border border-gray-500 hover:bg-bg-100" },
    bgHover: { true: "hover:bg-bg-100" },
    tight: { true: "gap-[5px]" },
    large: { true: "text-base" },
  },
});

const buttonIcon = tv({
  base: "text-sm",
  variants: {
    // later wins when several are set
    secondary: { true: "text-orange" },
    light: { true: "text-gray-500" },
    active: { true: "text-primary-500" },
    red: { true: "text-red" },
    small: { true: "text-xs" },
    large: { true: "text-base" },
  },
});

export interface IconButtonPropsT extends BasicButtonProps {
  iconProps?: IconStyleProps;
  active?: boolean;
  large?: boolean;
  small?: boolean;
  icon: IconProp;
  secondary?: boolean;
  tight?: boolean;
  red?: boolean;
  left?: boolean;
  frame?: boolean;
  bare?: boolean;
  light?: boolean;
  fixedIconWidth?: number;
  bgHover?: boolean;
  iconColor?: string;
}

// A button that is prefixed by an icon
const IconButton = ({
  ref,
  icon,
  active,
  red,
  large,
  left,
  children,
  tight,
  iconProps,
  small,
  secondary,
  light,
  fixedIconWidth,
  bgHover,
  iconColor,
  frame,
  className,
  ...restProps
}: IconButtonPropsT & { ref?: Ref<HTMLButtonElement> }) => {
  const iconElement = useMemo(() => {
    const iconEl = (
      <FaIcon
        left={left}
        icon={icon}
        {...iconProps}
        className={buttonIcon({
          secondary,
          light,
          active,
          red,
          small,
          large,
        })}
        style={
          iconColor
            ? { color: iconColor, ...iconProps?.style }
            : iconProps?.style
        }
      />
    );

    return fixedIconWidth ? (
      <span className="inline-block" style={{ width: fixedIconWidth }}>
        {iconEl}
      </span>
    ) : (
      iconEl
    );
  }, [
    icon,
    active,
    red,
    large,
    left,
    iconProps,
    small,
    secondary,
    light,
    fixedIconWidth,
    iconColor,
  ]);

  return (
    <BasicButton
      active={active}
      secondary={secondary}
      large={large}
      {...restProps}
      className={iconButton({
        secondary,
        active,
        red,
        frame,
        bgHover,
        tight,
        large,
        className,
      })}
      ref={ref}
    >
      {iconElement}
      {children && (
        <span className="flex items-center gap-[5px]">{children}</span>
      )}
    </BasicButton>
  );
};

export default memo(IconButton);
