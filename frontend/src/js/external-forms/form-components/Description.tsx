import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const description = tv({
  base: ["mx-[10px] mb-[10px] last:mb-0", "text-sm"],
});

export const Description = ({ className, ...props }: ComponentProps<"p">) => (
  <p className={description({ className })} {...props} />
);
