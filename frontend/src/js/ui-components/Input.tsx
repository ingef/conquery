import type { ReactNode, Ref } from "react";
import {
  Group,
  Button as RacButton,
  type ButtonProps as RacButtonProps,
  Input as RacInput,
  type InputProps as RacInputProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const frame = tv({
  base: [
    "flex items-center",
    "h-[30px]",
    "min-w-0",
    "rounded",
    "border border-gray-400",
    "bg-white",
    "text-sm text-gray-800",
    "data-focus-visible:outline-2 data-focus-visible:outline-primary-500",
    "data-disabled:opacity-50",
    "data-invalid:border-red",
  ],
});

const input = tv({
  base: [
    "h-full w-full min-w-0",
    "px-[10px]",
    "bg-transparent",
    "outline-none",
    "placeholder:text-gray-400",
    "disabled:cursor-not-allowed",
    "[&::-webkit-search-cancel-button]:hidden",
  ],
});

const addon = tv({
  base: ["flex items-center", "shrink-0", "gap-[2px]", "px-[3px]"],
});

// a small square button inside the frame: clear, open, calendar
const inputButton = tv({
  base: [
    "inline-flex items-center justify-center",
    "size-6 shrink-0",
    "rounded",
    "text-gray-800",
    "cursor-pointer",
    "hover:bg-gray-50",
    "disabled:cursor-not-allowed disabled:opacity-40",
  ],
});

export interface InputProps extends Omit<RacInputProps, "className" | "style"> {
  /** inside the frame, before the text: an icon button */
  addonLeft?: ReactNode;
  /** inside the frame, after the text: a unit, an icon button */
  addonRight?: ReactNode;
  isDisabled?: boolean;
  isInvalid?: boolean;
  ref?: Ref<HTMLInputElement>;
}

/** The text input of a field: a 30 px frame around react-aria's Input, with room for addons. */
export const Input = ({
  addonLeft,
  addonRight,
  isDisabled,
  isInvalid,
  ...props
}: InputProps) => (
  <Group className={frame()} isDisabled={isDisabled} isInvalid={isInvalid}>
    {addonLeft && <span className={addon()}>{addonLeft}</span>}
    <RacInput className={input()} {...props} />
    {addonRight && <span className={addon()}>{addonRight}</span>}
  </Group>
);

/** an icon button inside the input frame; give it an aria-label */
export const InputButton = (
  props: Omit<RacButtonProps, "className" | "style">,
) => <RacButton className={inputButton()} {...props} />;

/** a unit or similar text after the input's text */
export const InputText = ({ children }: { children: ReactNode }) => (
  <span className="px-[7px]">{children}</span>
);
