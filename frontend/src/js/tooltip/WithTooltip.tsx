import { css, useTheme } from "@emotion/react";
import styled from "@emotion/styled";
import Tippy from "@tippyjs/react";
import { memo, ReactElement, useMemo } from "react";
import "tippy.js/dist/tippy.css";
import "tippy.js/themes/light.css";

/* !important: to override inline styles by tippyjs/react */
export const tippyjsReactOverrides = css`
  div[data-tippy-root] {
    box-shadow: 0 0 8px rgba(0, 0, 0, 0.18);
    max-width: 700px;

    > div {
      max-width: inherit !important;
      width: 100%;
      padding: 0px 8px;
    }
  }
`;

const Text = styled("div")<{ wide?: boolean }>`
  max-width: ${({ wide }) => (wide ? "700px" : "400px")};
  text-align: left;
  font-size: 16px;
  font-weight: 400;
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

  // Some others are possible in @tippyjs/react, but those should be enough
  // default: "auto"
  placement?: "auto" | "top" | "bottom" | "left" | "right";
}

// Show and hide duration
const zeroDuration = [0, 0] as [number, number];

const WithTooltip = ({
  className,
  children,
  text,
  html,
  lazy,
  wide,
  placement,
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

  if (!text && !html) return <>{children}</>;

  return (
    <Tippy
      className={className}
      duration={zeroDuration}
      content={content}
      placement={placement}
      theme="light"
      delay={lazy ? ([1000, 0] as [number, number]) : 0}
    >
      {children}
    </Tippy>
  );
};

export default memo(WithTooltip);
