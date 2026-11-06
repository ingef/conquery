import type { ReactNode } from "react";
import { tv } from "tailwind-variants";

const label = tv({
  variants: {
    isHighlighted: {
      true: ["rounded", "bg-gray-50", "px-[3px] py-0"],
    },
  },
});

const HighlightableLabel = ({
  isHighlighted,
  className,
  children,
}: {
  children: ReactNode;
  className?: string;
  isHighlighted?: boolean;
}) => {
  return (
    <span className={label({ isHighlighted, className })}>{children}</span>
  );
};

export default HighlightableLabel;
