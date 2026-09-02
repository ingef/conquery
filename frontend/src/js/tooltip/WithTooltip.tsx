import { useTheme } from "@emotion/react";
import styled from "@emotion/styled";
import Tippy, { type TippyProps } from "@tippyjs/react";
import { memo, type ReactElement, type Ref, useMemo } from "react";
import "tippy.js/dist/tippy.css";
import "tippy.js/themes/light.css";

const Text = styled("div")<{ wide?: boolean }>`
  max-width: ${({ wide }) => (wide ? "700px" : "400px")};
  text-align: left;
  font-size: 16px;
  font-weight: 400;
  padding: 8px 14px;
  p,
  h3,
  h4 {
    color: ${({ theme }) => theme.col.black};
    line-height: 1.3;
    margin: 8px 0 0;
  }
  p,
  h3,
  li {
    font-size: ${({ theme }) => theme.font.sm};
  }
  ul {
    margin: 6px 0;
    padding-left: 16px;
  }
  li {
    line-height: 1.3;
    margin-bottom: 5px;
  }
`;

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
  text,
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
  const theme = useTheme();

  const content = useMemo(() => {
    return text ? (
      <Text
        theme={theme}
        wide={wide}
        // biome-ignore lint/security/noDangerouslySetInnerHtml: tooltip text is concept metadata from the backend
        dangerouslySetInnerHTML={{ __html: text }}
      />
    ) : (
      html
    );
  }, [theme, wide, text, html]);

  const delay = useMemo(
    () => (lazy ? ([1000, 0] as [number, number]) : 0),
    [lazy],
  );

  if (!text && !html) return <>{children}</>;

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
