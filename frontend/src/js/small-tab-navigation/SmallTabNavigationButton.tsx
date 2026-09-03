import type { ButtonHTMLAttributes, Ref } from "react";
import { tv } from "tailwind-variants";

import { HoverNavigatable } from "./HoverNavigatable";

const bottomBorder = [
  "after:content-['']",
  "after:absolute",
  "after:inset-x-0",
  "after:bottom-0",
  "after:h-[3px]",
];

// tailwind only generates classes it finds literally in the source,
// so this cannot be derived from `bottomBorder` at runtime
const bottomBorderOnHover = [
  "hover:after:content-['']",
  "hover:after:absolute",
  "hover:after:inset-x-0",
  "hover:after:bottom-0",
  "hover:after:h-[3px]",
];

const button = tv({
  base: [
    "relative",
    "rounded-t",
    "mx-[2px]",
    "transition-[border] duration-100 ease-in-out",
  ],
  variants: {
    size: {
      L: ["h-[30px]", "px-[10px]", "text-sm"],
      M: ["h-[26px]", "px-[3px]", "text-xs", "uppercase"],
    },
    primary: {
      true: ["translate-y-px", "border border-b-0"],
    },
    selected: { true: "", false: "" },
    // color of the selected bottom bar, keyed by the tab's value
    highlight: { own: "", system: "", default: "" },
  },
  compoundVariants: [
    {
      primary: true,
      selected: true,
      class: ["border-gray-500", "bg-bg-50"],
    },
    {
      primary: true,
      selected: false,
      class: ["border-transparent", "hover:border-gray-400 hover:border-b-0"],
    },
    {
      primary: false,
      selected: true,
      class: bottomBorder,
    },
    {
      primary: false,
      selected: true,
      highlight: "own",
      class: "after:bg-primary-500",
    },
    {
      primary: false,
      selected: true,
      highlight: "system",
      class: "after:bg-gray-100",
    },
    {
      primary: false,
      selected: true,
      highlight: "default",
      class: "after:bg-gray-800",
    },
    {
      primary: false,
      selected: false,
      class: [
        "text-gray-500",
        ...bottomBorderOnHover,
        "hover:after:bg-gray-100",
      ],
    },
  ],
});

const SmallTabNavigationButton = ({
  ref,
  value,
  children,
  size,
  isSelected,
  onClick,
  variant,
  ...props
}: {
  ref?: Ref<HTMLButtonElement>;

  value: string;
  size: "M" | "L";
  isSelected?: boolean;
  onClick: () => void;
  children?: React.ReactNode;
  variant: "primary" | "secondary";
} & Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  "onClick" | "value" | "children"
>) => {
  const highlight =
    value === "own" ? "own" : value === "system" ? "system" : "default";

  return (
    <HoverNavigatable triggerNavigate={onClick}>
      <button
        ref={ref}
        {...props}
        className={button({
          size,
          primary: variant === "primary",
          selected: !!isSelected,
          highlight,
        })}
        type="button"
        onClick={onClick}
      >
        {children}
      </button>
    </HoverNavigatable>
  );
};

export default SmallTabNavigationButton;
