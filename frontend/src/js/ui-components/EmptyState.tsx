import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

const root = tv({
  base: [
    "flex flex-col items-center justify-center",
    "grow",
    "gap-4",
    "px-5 py-10",
    "text-center text-sm",
    "text-gray-500",
  ],
});

/** an icon with a short message below, for an area without content */
export const EmptyState = ({
  icon: StateIcon,
  children,
}: {
  icon: LucideIcon;
  children: ReactNode;
}) => (
  <div className={root()}>
    <StateIcon className="size-10 text-gray-100" />
    <p>{children}</p>
  </div>
);
