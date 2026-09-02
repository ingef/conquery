import Tippy, { type TippyProps } from "@tippyjs/react";
import { memo, type ReactElement, type Ref, useMemo } from "react";
import { tv } from "tailwind-variants";
import "tippy.js/dist/tippy.css";
import "tippy.js/themes/light.css";

const text = tv({
  base: [
    "max-w-[400px]",
    "text-left",
    "text-base",
    "font-normal",
    "px-[14px] py-2",
    "[&_p]:text-gray-800 [&_h3]:text-gray-800 [&_h4]:text-gray-800",
    "[&_p]:leading-[1.3] [&_h3]:leading-[1.3] [&_h4]:leading-[1.3]",
    "[&_p]:mt-2 [&_h3]:mt-2 [&_h4]:mt-2",
    "[&_p]:text-sm [&_h3]:text-sm [&_li]:text-sm",
    "[&_ul]:my-[6px] [&_ul]:pl-4",
    "[&_li]:leading-[1.3] [&_li]:mb-[5px]",
  ],
  variants: {
    wide: { true: "max-w-[700px]" },
  },
});

interface Props {
  className?: string;
  text?: string;
  html?: ReactElement;
  lazy?: boolean;
  wide?: boolean;
  children?: ReactElement;
  interactive?: boolean;
  trigger?: string;
  arrow?: TippyProps["arrow"];
  offset?: TippyProps["offset"];
  hideOnClick?: TippyProps["hideOnClick"];
  popperOptions?: TippyProps["popperOptions"];

  // Some others are possible in @tippyjs/react, but those should be enough
  // default: "auto"
  placement?: "auto" | "top" | "bottom" | "left" | "right";
}

// Show and hide duration
const shortDuration = [100, 100] as [number, number];

const WithTooltip = ({
  ref,
  className,
  children,
  text: textProp,
  html,
  lazy,
  wide,
  placement,
  interactive,
  trigger,
  arrow,
  offset,
  hideOnClick,
  popperOptions,
}: Props & { ref?: Ref<HTMLElement> }) => {
  const content = useMemo(() => {
    return textProp ? (
      <div
        className={text({ wide })}
        // biome-ignore lint/security/noDangerouslySetInnerHtml: tooltip text is concept metadata from the backend
        dangerouslySetInnerHTML={{ __html: textProp }}
      />
    ) : (
      html
    );
  }, [wide, textProp, html]);

  const delay = useMemo(
    () => (lazy ? ([1000, 0] as [number, number]) : 0),
    [lazy],
  );

  if (!textProp && !html) return <>{children}</>;

  return (
    <Tippy
      className={className}
      duration={shortDuration}
      content={content}
      placement={placement}
      theme="light"
      delay={delay}
      interactive={interactive}
      trigger={trigger}
      arrow={arrow}
      offset={offset}
      ref={ref}
      zIndex={9999}
      popperOptions={popperOptions}
      hideOnClick={hideOnClick}
    >
      {children}
    </Tippy>
  );
};

export default memo(WithTooltip);
