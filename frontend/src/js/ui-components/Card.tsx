import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

const card = tv({
  base: ["rounded", "border border-gray-100", "bg-white", "p-3"],
});

/**
 * A white surface that groups content on a page background. Layout inside
 * and around it belongs to the content and the parent.
 */
export const Card = ({ children }: { children: ReactNode }) => (
  <div className={card()}>{children}</div>
);
