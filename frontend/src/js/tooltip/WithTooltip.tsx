import { useTheme } from "@emotion/react";
import styled from "@emotion/styled";
import React, { FC, ReactElement } from "react";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

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

interface PropsT {
  className?: string;
  place?: "bottom" | "left" | "right" | "top";
  text?: string;
  html?: ReactElement;
  lazy?: boolean;
  wide?: boolean;
}

const WithTooltip: FC<PropsT> = ({
  className,
  children,
  place,
  text,
  html,
  lazy,
  wide,
}) => {
  const theme = useTheme();

  if (!text && !html) return <>{children}</>;

  const delayProps = {
    // For some reason, supplying delay as an array is the only way
    // to get the hide delay to work. The types seem to be outdated.
    // Check this for further info:
    // https://github.com/tvkhoa/react-tippy/issues/52#issuecomment-406419701
    delay: lazy ? (([1000, 0] as unknown) as number) : 0,
    // So this doesn't work, but let's supply it anyways:
    hideDelay: 0,
  };

  return (
    <Tooltip
      className={className}
      position={place || "top"}
      arrow={true}
      duration={0}
      hideDuration={0}
      html={
        text ? (
          <Text
            theme={theme}
            wide={wide}
            dangerouslySetInnerHTML={{ __html: text }}
          />
        ) : (
          html
        )
      }
      theme="light"
      {...delayProps}
    >
      {children}
    </Tooltip>
  );
};

export default WithTooltip;
