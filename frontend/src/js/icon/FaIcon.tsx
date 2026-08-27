import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { ComponentProps, Ref } from "react";

import { tv } from "tailwind-variants";

export interface IconStyleProps {
  left?: boolean;
  center?: boolean;
  right?: boolean;
  white?: boolean;
  red?: boolean;
  light?: boolean;
  gray?: boolean;
  main?: boolean;
  active?: boolean;
  disabled?: boolean;
  tiny?: boolean;
  large?: boolean;
  small?: boolean;
  style?: ComponentProps<typeof FontAwesomeIcon>["style"];
}

export interface FaIconPropsT extends IconStyleProps {
  icon: IconProp;
  className?: string;
}

const icon = tv({
  base: [
    "w-[initial]!",
    "text-sm",
    "text-gray-800",
    "[&.fa-spinner]:animate-spin-fast",
  ],
  variants: {
    left: { true: "pr-[10px]" },
    right: { true: "pl-[10px]" },
    center: { true: "text-center" },
    // later wins when both are set
    tiny: { true: "text-[11px]" },
    large: { true: "text-base" },
    disabled: { true: "cursor-not-allowed" },
  },
});

// First matching flag wins, in this order
const colorClass = (p: IconStyleProps) => {
  if (p.disabled) return "text-gray-400";
  if (p.red) return "text-red";
  if (p.gray) return "text-gray-500";
  if (p.active) return "text-primary-500";
  if (p.white) return "text-white";
  if (p.light) return "text-primary-100";
  if (p.main) return "text-primary-200";
  return undefined;
};

const FaIcon = ({
  ref,
  icon: iconProp,
  className,
  left,
  center,
  right,
  white,
  red,
  light,
  gray,
  main,
  active,
  disabled,
  tiny,
  large,
  small: _small, // only meaningful to IconButton
  style,
}: FaIconPropsT & { ref?: Ref<SVGSVGElement> }) => {
  return (
    <FontAwesomeIcon
      ref={ref}
      className={icon({
        left,
        center,
        right,
        tiny,
        large,
        disabled,
        className: [
          colorClass({ white, red, light, gray, main, active, disabled }),
          className,
        ],
      })}
      icon={iconProp}
      style={style}
    />
  );
};

export default FaIcon;
