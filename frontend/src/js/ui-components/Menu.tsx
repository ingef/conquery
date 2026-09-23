import {
  type PopoverProps,
  Menu as RacMenu,
  MenuItem as RacMenuItem,
  type MenuItemProps as RacMenuItemProps,
  type MenuProps as RacMenuProps,
  Separator,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { Popover } from "./Popover";
import { textStyle } from "./Typography";

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
    "h-[30px] px-3",
    textStyle({ size: 2, tone: "default" }),
    "whitespace-nowrap",
    "cursor-pointer",
    "outline-none",
    "opacity-75",
    "data-focused:opacity-100 data-focused:bg-gray-50",
    "data-disabled:cursor-not-allowed data-disabled:opacity-40",
    "transition-[opacity,background-color] duration-100",
  ],
  variants: {
    danger: { true: "text-red" },
  },
});

/**
 * A list of actions or links that opens next to its trigger, composed the way
 * react-aria does it. The popover is part of the Menu:
 *
 *   <MenuTrigger>
 *     <Button … />
 *     <Menu aria-label="…" onAction={(key) => …}>
 *       <MenuItem id="…">…</MenuItem>
 *       <MenuItem href="…">…</MenuItem>
 *     </Menu>
 *   </MenuTrigger>
 *
 * A `MenuSeparator` divides groups of items.
 *
 * MenuTrigger comes from react-aria-components; a Button or ToggleButton
 * is its trigger without further wiring. Items focus on hover, arrow keys
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

export const MenuSeparator = () => (
  <Separator className="my-1 h-px shrink-0 border-0 bg-gray-100" />
);
