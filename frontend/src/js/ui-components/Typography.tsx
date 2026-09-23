import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

/* The type scale: tailwind's sizes with their line-heights. Controls compose
   these presets, free text renders the H and C components below. */

export const headingStyle = tv({
  base: "font-medium",
  variants: {
    level: {
      1: "text-2xl",
      2: "text-xl",
      3: "text-base",
      4: "text-sm",
      5: "text-xs",
    },
    tone: {
      default: "text-gray-800",
      muted: "text-gray-500",
    },
    truncate: { true: "truncate" },
  },
  defaultVariants: { tone: "default" },
});

export const textStyle = tv({
  variants: {
    size: {
      1: "text-base",
      2: "text-sm",
      3: "text-xs",
    },
    // no default: text inherits the color of its container
    tone: {
      default: "text-gray-800",
      muted: "text-gray-500",
      danger: "text-red",
      success: "text-green",
      primary: "text-primary-500",
    },
    strong: { true: "font-medium" },
    truncate: { true: "truncate" },
  },
});

type HeadingLevel = 1 | 2 | 3 | 4 | 5;

export interface HeadingProps
  extends Omit<ComponentProps<"h1">, "className" | "style"> {
  /** the element, when the document outline differs from the look */
  as?: "h1" | "h2" | "h3" | "h4" | "h5" | "h6" | "div" | "span";
  tone?: "default" | "muted";
  truncate?: boolean;
}

const Heading = ({
  level,
  as: Tag = `h${level}`,
  tone,
  truncate,
  ...props
}: HeadingProps & { level: HeadingLevel }) => (
  <Tag className={headingStyle({ level, tone, truncate })} {...props} />
);

/** 24 px */
export const H1 = (props: HeadingProps) => <Heading level={1} {...props} />;
/** 20 px */
export const H2 = (props: HeadingProps) => <Heading level={2} {...props} />;
/** 16 px */
export const H3 = (props: HeadingProps) => <Heading level={3} {...props} />;
/** 14 px */
export const H4 = (props: HeadingProps) => <Heading level={4} {...props} />;
/** 12 px */
export const H5 = (props: HeadingProps) => <Heading level={5} {...props} />;

type TextSize = 1 | 2 | 3;

export interface TextProps
  extends Omit<ComponentProps<"p">, "className" | "style"> {
  /** `span` for text inside a line, `p` (the default) for a block */
  as?: "p" | "span" | "div" | "code";
  tone?: "default" | "muted" | "danger" | "success" | "primary";
  /** weight 500, the one emphasis weight */
  strong?: boolean;
  /** one line, cut with an ellipsis; needs a width from the parent */
  truncate?: boolean;
}

const Text = ({
  size,
  as: Tag = "p",
  tone,
  strong,
  truncate,
  ...props
}: TextProps & { size: TextSize }) => (
  <Tag className={textStyle({ size, tone, strong, truncate })} {...props} />
);

/** 16 px: prose that stands on its own, like a form's description */
export const C1 = (props: TextProps) => <Text size={1} {...props} />;
/** 14 px: the default content size */
export const C2 = (props: TextProps) => <Text size={2} {...props} />;
/** 12 px: secondary information next to content */
export const C3 = (props: TextProps) => <Text size={3} {...props} />;
