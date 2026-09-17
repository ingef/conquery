import type { LucideIcon, LucideProps } from "lucide-react";
import { type ClassValue, tv } from "tailwind-variants";

const root = tv({
  base: "[&.lucide-loader-circle]:animate-spin-fast",
  variants: {
    filled: { true: "fill-current" },
  },
});

/**
 * Lucide icon.
 *
 * - size and stroke width: app-wide, `--icon-size` and `--icon-stroke-width`
 *   in index.css. Override with a `size-*` class only where the icon is a
 *   picture (empty states, big status marks), with a `stroke-*` class only
 *   for a deliberate weight.
 * - color: `currentColor`, inherited from the surrounding text. A button
 *   colors its icon through its own text color.
 * - filled: the "on" look of an icon that shows a state.
 */
export const Icon = ({
  icon: IconComponent,
  filled,
  className,
  ...props
}: Omit<
  LucideProps,
  | "ref"
  | "className"
  | "size"
  | "width"
  | "height"
  | "strokeWidth"
  | "absoluteStrokeWidth"
  | "nonScalingStroke"
> & {
  icon: LucideIcon;
  filled?: boolean;
  className?: ClassValue;
}) => (
  // the stroke keeps its width in px however large the icon is drawn
  <IconComponent
    nonScalingStroke
    className={root({ filled, className })}
    {...props}
  />
);
