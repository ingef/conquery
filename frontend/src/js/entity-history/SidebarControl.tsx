import {
  Button as RacButton,
  type ButtonProps as RacButtonProps,
  ToggleButton as RacToggleButton,
  type ToggleButtonProps as RacToggleButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

// The history sidebar's vertical toolbar: square controls, a selected one
// shows in the primary color. Local to the history, not a design-system part.
const control = tv({
  base: [
    "inline-flex items-center justify-center",
    "size-[30px]",
    "rounded",
    "border border-transparent",
    "text-gray-800",
    "cursor-pointer",
    "hover:bg-gray-50",
    "data-selected:bg-gray-100 data-selected:text-primary-500",
    "transition-colors duration-100",
  ],
});

export const SidebarToggle = (props: RacToggleButtonProps) => (
  <RacToggleButton className={control()} {...props} />
);

export const SidebarAction = (props: RacButtonProps) => (
  <RacButton className={control()} {...props} />
);
