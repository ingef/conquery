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
 * dismissed on Escape or a click outside. Menu renders it itself, so callers
 * don't need it; it is exported for further overlay components.
 */
export const Popover = ({
  className,
  offset = 5,
  ...props
}: Omit<RacPopoverProps, "className"> & { className?: string }) => (
  <RacPopover offset={offset} className={popover({ className })} {...props} />
);
