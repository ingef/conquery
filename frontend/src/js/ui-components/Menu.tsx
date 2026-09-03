import {
  type PopoverProps,
  Menu as RacMenu,
  MenuItem as RacMenuItem,
  type MenuItemProps as RacMenuItemProps,
  type MenuProps as RacMenuProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { Popover } from "./Popover";

const menu = tv({
  base: [
    "flex flex-col",
    "gap-[2px]",
    "p-2",
    "max-h-[60vh]",
    "overflow-y-auto",
    "outline-none",
  ],
});

const menuItem = tv({
  base: [
    "flex items-center",
    "gap-[10px]",
    "rounded",
    "px-[15px] py-2",
    // explicit: through the portal an item would inherit body's line-height and light weight
    "text-sm leading-none font-normal",
    "text-gray-800",
    "whitespace-nowrap",
    "cursor-pointer",
    "outline-none",
    "opacity-75",
    "data-focused:opacity-100 data-focused:bg-gray-50",
    "data-disabled:cursor-not-allowed data-disabled:opacity-40",
    "transition-[opacity,background-color] duration-100",
  ],
  variants: {
    // the svg selector beats the colour FaIcon still sets on itself
    danger: { true: "text-red [&_svg]:text-red" },
  },
});

/** Fixed-width box so icons of different widths keep the labels aligned. */
export const menuItemIcon = tv({
  base: ["inline-flex justify-center", "w-4 shrink-0"],
});

/**
 * A list of actions or links that opens next to its trigger, composed the way
 * react-aria does it. The popover is part of the Menu:
 *
 *   <MenuTrigger>
 *     <IconButton … />
 *     <Menu aria-label="…" onAction={(key) => …}>
 *       <MenuItem id="…">…</MenuItem>
 *       <MenuItem href="…">…</MenuItem>
 *     </Menu>
 *   </MenuTrigger>
 *
 * MenuTrigger comes from react-aria-components; buttons built on BasicButton
 * are its trigger without further wiring. Items focus on hover, arrow keys
 * move between them, the menu closes after an action. `placement` positions
 * the menu relative to the trigger (default below, start-aligned).
 */
export const Menu = ({
  className,
  placement,
  ...props
}: Omit<RacMenuProps<object>, "className"> & {
  className?: string;
  placement?: PopoverProps["placement"];
}) => (
  <Popover placement={placement}>
    <RacMenu className={menu({ className })} {...props} />
  </Popover>
);

export const MenuItem = ({
  className,
  danger,
  ...props
}: Omit<RacMenuItemProps, "className"> & {
  className?: string;
  danger?: boolean;
}) => <RacMenuItem className={menuItem({ danger, className })} {...props} />;
