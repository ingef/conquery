import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

import type { Headline as HeadlineField } from "../config-types";

const HEADLINE_DOM = {
  h1: "h3" as const,
  h2: "h4" as const,
  h3: "h5" as const,
};

export const getHeadlineFieldAs = (headline: HeadlineField) => {
  if (!headline.style?.size) return "h3";

  // To convert the "simplified" headline type to the real DOM element type
  return HEADLINE_DOM[headline.style.size];
};

const headline = tv({
  base: [
    "relative",
    "flex items-center",
    "gap-[10px]",
    "leading-none",
    "text-gray-800",
    // wins over the size margins: :first-child raises specificity
    "first:mt-0",
  ],
  variants: {
    size: {
      h1: ["text-xl", "font-normal", "mt-5 mb-[5px] ml-0"],
      h2: ["text-base", "font-normal", "mt-[10px] mb-[3px] ml-[10px]"],
      h3: ["text-sm", "font-bold", "mt-[10px] mb-[3px] ml-[10px]"],
    },
  },
  defaultVariants: { size: "h1" },
});

export const Headline = ({
  as: Component = "h3",
  size,
  className,
  ...props
}: ComponentProps<"h3"> & {
  as?: "h3" | "h4" | "h5";
  size?: "h1" | "h2" | "h3";
}) => <Component className={headline({ size, className })} {...props} />;

const headlineIndex = tv({
  base: [
    "flex items-center justify-center",
    "px-[10px]",
    "text-xl",
    "border-r-[3px] border-gray-400",
    "text-gray-400",
  ],
});

export const HeadlineIndex = ({
  className,
  ...props
}: ComponentProps<"span">) => (
  <span className={headlineIndex({ className })} {...props} />
);
