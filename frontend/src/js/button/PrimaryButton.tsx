import type { Ref } from "react";

import { tv } from "../tv";

import BasicButton, { type BasicButtonProps } from "./BasicButton";

const primaryButton = tv({
  base: [
    "text-white",
    "bg-primary-500 bg-clip-padding",
    "border border-primary-500",
    "hover:opacity-90",
  ],
});

const PrimaryButton = ({
  className,
  ...props
}: BasicButtonProps & { ref?: Ref<HTMLButtonElement> }) => (
  <BasicButton className={primaryButton({ className })} {...props} />
);

export default PrimaryButton;
