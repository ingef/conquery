import type { ElementType, HTMLAttributes, ReactNode, Ref } from "react";
import { mergeProps, useFocusable, useObjectRef } from "react-aria";
import {
  OverlayArrow,
  Tooltip as RacTooltip,
  type TooltipProps as RacTooltipProps,
  TooltipTrigger as RacTooltipTrigger,
  type TooltipTriggerComponentProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const tooltip = tv({
  base: [
    "z-[9999]",
    "max-w-[400px]",
    "rounded",
    "bg-white",
    "shadow-[0_0_8px_rgba(0,0,0,0.18)]",
    "px-[14px] py-2",
    "text-left",
    "text-base",
    "font-normal",
    "text-gray-800",
    "data-entering:animate-fade-in",
    "data-exiting:animate-fade-out",
    // rich content
    "[&_p]:text-sm [&_h3]:text-sm [&_li]:text-sm",
    "[&_p]:leading-[1.3] [&_h3]:leading-[1.3] [&_h4]:leading-[1.3]",
    "[&_p]:mt-2 [&_h3]:mt-2 [&_h4]:mt-2",
    "[&_ul]:my-[6px] [&_ul]:pl-4",
    "[&_li]:leading-[1.3] [&_li]:mb-[5px]",
  ],
  variants: {
    wide: { true: "max-w-[700px]" },
  },
});

const arrow = tv({
  base: [
    "*:block",
    "*:fill-white",
    "data-[placement=bottom]:*:rotate-180",
    "data-[placement=left]:*:-rotate-90",
    "data-[placement=right]:*:rotate-90",
  ],
});

/**
 * Wraps a trigger element and its Tooltip:
 *
 *   <TooltipTrigger>
 *     <IconButton … />
 *     <Tooltip>{text}</Tooltip>
 *   </TooltipTrigger>
 *
 * Buttons based on BasicButton attach themselves to the trigger.
 * Other elements need a TooltipTarget (or react-aria's Focusable for native buttons).
 */
export const TooltipTrigger = ({
  delay = 500,
  ...props
}: TooltipTriggerComponentProps) => (
  <RacTooltipTrigger delay={delay} {...props} />
);

export const Tooltip = ({
  children,
  className,
  wide,
  offset = 8,
  ...props
}: Omit<RacTooltipProps, "className" | "children"> & {
  children?: ReactNode;
  className?: string;
  wide?: boolean;
}) => {
  if (!children) return null;

  return (
    <RacTooltip
      offset={offset}
      className={tooltip({ wide, className })}
      {...props}
    >
      <OverlayArrow className={arrow()}>
        <svg width={8} height={8} viewBox="0 0 8 8" aria-hidden="true">
          <path d="M0 0 L4 4 L8 0" />
        </svg>
      </OverlayArrow>
      {children}
    </RacTooltip>
  );
};

/**
 * Makes a non-button element the element a TooltipTrigger attaches to.
 * Static content stays out of the tab order with `excludeFromTabOrder`.
 */
export const TooltipTarget = ({
  as: Tag = "span",
  ref,
  excludeFromTabOrder,
  ...props
}: HTMLAttributes<HTMLElement> & {
  as?: ElementType;
  ref?: Ref<HTMLElement>;
  excludeFromTabOrder?: boolean;
}) => {
  const domRef = useObjectRef(ref);
  const { focusableProps } = useFocusable({ excludeFromTabOrder }, domRef);

  return <Tag ref={domRef} {...mergeProps(focusableProps, props)} />;
};
