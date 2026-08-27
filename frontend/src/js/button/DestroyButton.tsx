import type { ComponentProps } from "react";

import { tv } from "../tv";

import { TransparentButton } from "./TransparentButton";

const destroyButton = tv({
  base: [
    "text-red hover:text-white active:text-white focus:text-white",
    "border-2 border-red",
    "hover:bg-red active:bg-red focus:bg-red",
  ],
});

export const DestroyButton = ({
  className,
  ...props
}: ComponentProps<typeof TransparentButton>) => (
  <TransparentButton className={destroyButton({ className })} {...props} />
);
