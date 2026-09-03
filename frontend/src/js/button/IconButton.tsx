import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { memo, type Ref } from "react";
import { tv } from "tailwind-variants";

import { Icon } from "../ui-components/Icon";

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
    active: { true: "text-primary-500" },
    red: { true: "text-red" },
    frame: { true: "opacity-100 border border-gray-500 hover:bg-bg-100" },
    bgHover: { true: "hover:bg-bg-100" },
    tight: { true: "gap-[5px]" },
    large: { true: "text-base" },
  },
});

// The button's text color reaches the icon; this is the exception.
const buttonIcon = tv({
  variants: {
    light: { true: "text-gray-500" },
  },
});

export interface IconButtonPropsT extends Omit<BasicButtonProps, "small"> {
  icon: IconProp;
  active?: boolean;
  large?: boolean;
  tight?: boolean;
  red?: boolean;
  frame?: boolean;
  bare?: boolean;
  light?: boolean;
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
  children,
  tight,
  light,
  bgHover,
  iconColor,
  frame,
  className,
  ...restProps
}: IconButtonPropsT & { ref?: Ref<HTMLButtonElement> }) => (
  <BasicButton
    active={active}
    large={large}
    {...restProps}
    className={iconButton({
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
    <Icon
      icon={icon}
      className={buttonIcon({ light })}
      style={iconColor ? { color: iconColor } : undefined}
    />
    {children && (
      <span className="flex items-center gap-[5px]">{children}</span>
    )}
  </BasicButton>
);

export default memo(IconButton);
