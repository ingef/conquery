import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const inABox = tv({
  base: ["inline-flex items-center justify-center", "h-6 w-6", "rounded"],
});

export const InABox = ({ className, ...props }: ComponentProps<"div">) => (
  <div className={inABox({ className })} {...props} />
);
