import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

const card = tv({
  base: ["rounded", "border border-gray-100", "bg-white"],
  variants: {
    // `none` for content that brings its own padding, like a table or buckets
    padding: {
      md: "p-[14px]",
      none: "",
    },
  },
  defaultVariants: { padding: "md" },
});

/**
 * A white surface that groups content on a page background. Layout inside
 * and around it belongs to the content and the parent.
 */
export const Card = ({
  padding,
  children,
}: {
  padding?: "md" | "none";
  children: ReactNode;
}) => <div className={card({ padding })}>{children}</div>;
