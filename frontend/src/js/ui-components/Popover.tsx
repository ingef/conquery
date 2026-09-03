import {
  type DialogProps,
  Dialog as RacDialog,
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

const dialog = tv({ base: "outline-none" });

/**
 * Click-triggered overlay, composed the way react-aria does it:
 *
 *   <DialogTrigger>
 *     <IconButton … />
 *     <Popover>
 *       <Dialog aria-label="…">{({ close }) => …}</Dialog>
 *     </Popover>
 *   </DialogTrigger>
 *
 * DialogTrigger comes from react-aria-components; buttons built on
 * BasicButton are its trigger without further wiring.
 */
export const Popover = ({
  className,
  ...props
}: Omit<RacPopoverProps, "className"> & { className?: string }) => (
  <RacPopover className={popover({ className })} {...props} />
);

export const Dialog = ({ className, ...props }: DialogProps) => (
  <RacDialog className={dialog({ className })} {...props} />
);
