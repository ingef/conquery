import {
  Popover as RacPopover,
  type PopoverProps as RacPopoverProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const popover = tv({
  base: [
    "z-[9999]",
    "rounded",
    "bg-white",
    "shadow-[0_0_8px_rgba(0,0,0,0.18)]",
    "data-entering:animate-fade-in",
    "data-exiting:animate-fade-out",
  ],
});

/**
 * Container for click-triggered overlays, positioned next to its trigger and
 * dismissed on Escape or a click outside. Holds a Menu (see Menu.tsx).
 */
export const Popover = ({
  className,
  ...props
}: Omit<RacPopoverProps, "className"> & { className?: string }) => (
  <RacPopover className={popover({ className })} {...props} />
);
