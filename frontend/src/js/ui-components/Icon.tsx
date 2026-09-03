import { config } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import type { ComponentProps } from "react";
import { type ClassValue, tv } from "tailwind-variants";

// Font Awesome's stylesheet is imported in index.css into tailwind's base
// layer, so size utilities apply to icons. Keep it from injecting a second,
// unlayered copy.
config.autoAddCss = false;

const icon = tv({
  base: [
    // one square frame for all icons; a picture-like icon overrides it with a size-* class
    "size-3.5 shrink-0",
    "[&.fa-spinner]:animate-spin-fast",
  ],
});

/**
 * Font Awesome icon in the app's frame.
 *
 * - frame: 14 by 14 px, the glyph centered in it, so icons line up in
 *   rows, lists and menus regardless of glyph shape. Override with a
 *   `size-*` class only where the icon is a picture (empty states, big
 *   status marks).
 * - color: `currentColor`, inherited from the surrounding text. A button
 *   colors its icon through its own text color.
 */
export const Icon = ({
  className,
  ...props
}: Omit<ComponentProps<typeof FontAwesomeIcon>, "className"> & {
  className?: ClassValue;
}) => <FontAwesomeIcon className={icon({ className })} {...props} />;
