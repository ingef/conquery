import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";
import { Button } from "../Button";

import SelectListOption from "./SelectListOption";

const control = tv({
  base: [
    "flex items-center",
    "min-h-[30px]",
    "overflow-hidden",
    "rounded-[4px]",
    "border border-gray-500",
    "bg-white",
    // 2 px + the 24 px buttons + the border fill the 30 px exactly; wrapped
    // lines of chips keep off the edges
    "py-0.5 pr-[3px] pl-2",
    "focus:outline focus:outline-1 focus:outline-black",
  ],
  variants: {
    disabled: { true: "cursor-not-allowed" },
  },
});

export const Control = ({
  className,
  disabled,
  ...props
}: ComponentProps<"div"> & { disabled?: boolean }) => (
  <div className={control({ disabled, className })} {...props} />
);

const selectContainer = tv({ base: ["relative", "w-full"] });

export const SelectContainer = ({
  className,
  ...props
}: ComponentProps<"div">) => (
  <div className={selectContainer({ className })} {...props} />
);

const itemsInputContainer = tv({
  base: ["flex flex-wrap items-center", "gap-1", "w-full"],
});

export const ItemsInputContainer = ({
  className,
  ...props
}: ComponentProps<"div">) => (
  <div className={itemsInputContainer({ className })} {...props} />
);

const menuContainer = tv({
  base: ["absolute", "z-3", "w-full", "mt-[3px]", "pb-[10px]"],
});

export const MenuContainer = ({
  className,
  ...props
}: ComponentProps<"div">) => (
  <div className={menuContainer({ className })} {...props} />
);

const menu = tv({
  base: [
    "w-full",
    "rounded-[4px]",
    "shadow-[0_0_0_1px_hsl(0deg_0%_0%/10%),0_4px_11px_hsl(0deg_0%_0%/10%)]",
    "bg-bg-50",
  ],
});

export const Menu = ({ className, ...props }: ComponentProps<"div">) => (
  <div className={menu({ className })} {...props} />
);

const list = tv({
  base: [
    "p-[3px]",
    // remove the max-height once we use usePopper / portals for this
    "max-h-[300px]",
    "overflow-y-auto",
    "[-webkit-overflow-scrolling:touch]",
    "overscroll-contain",
  ],
  variants: {
    small: { true: "max-h-[140px]" },
  },
});

export const List = ({
  className,
  small,
  ...props
}: ComponentProps<"div"> & { small?: boolean }) => (
  <div className={list({ small, className })} {...props} />
);

const input = tv({
  base: [
    "grow",
    "w-0", // to fix default width
    "h-5",
    "border-0",
    "outline-none",
    "text-sm",
    "font-normal",
    "disabled:cursor-not-allowed disabled:pointer-events-none",
    "disabled:placeholder:opacity-50",
  ],
});

export const Input = ({ className, ...props }: ComponentProps<"input">) => (
  <input className={input({ className })} {...props} />
);

export const DropdownToggleButton = ({
  className,
  ...props
}: ComponentProps<typeof Button>) => (
  <Button intent="tertiary" size="sm" {...props} className={className} />
);

export const ResetButton = ({
  className,
  ...props
}: ComponentProps<typeof Button>) => (
  <Button intent="tertiary" size="sm" {...props} className={className} />
);

const verticalSeparator = tv({
  base: ["self-stretch", "shrink-0", "w-px", "my-[3px]", "bg-gray-100"],
});

export const VerticalSeparator = ({
  className,
  ...props
}: ComponentProps<"div">) => (
  <div className={verticalSeparator({ className })} {...props} />
);

const sxSelectListOption = tv({ base: "mb-[2px]" });

export const SxSelectListOption = ({
  className,
  ...props
}: ComponentProps<typeof SelectListOption>) => (
  <SelectListOption className={sxSelectListOption({ className })} {...props} />
);
