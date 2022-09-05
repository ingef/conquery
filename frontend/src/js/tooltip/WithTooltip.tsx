import { css, useTheme } from "@emotion/react";
import styled from "@emotion/styled";
import Tippy, { TippyProps } from "@tippyjs/react";
import { memo, ReactElement, useMemo } from "react";
import "tippy.js/dist/tippy.css";
import "tippy.js/themes/light.css";

/* !important: to override inline styles by tippyjs/react */
export const tippyjsReactOverrides = css`
  div[data-tippy-root] {
    max-width: 700px;
    box-shadow: 0 0 8px rgba(0, 0, 0, 0.18);
    border-radius: 3px;

    > div {
      box-shadow: none;
      max-width: inherit !important;
      width: 100%;
      padding: 0;
    }

    .tippy-content {
      padding: 0px;
      box-shadow: none;
    }
  }
`;

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

  // Some others are possible in @tippyjs/react, but those should be enough
  // default: "auto"
  placement?: "auto" | "top" | "bottom" | "left" | "right";
}

// Show and hide duration
const shortDuration = [100, 100] as [number, number];

const WithTooltip = ({
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
}: Props) => {
  const theme = useTheme();

  const content = useMemo(() => {
    return text ? (
      <Text
        theme={theme}
        wide={wide}
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
    >
      {children}
    </Tippy>
  );
};

export default memo(WithTooltip);
