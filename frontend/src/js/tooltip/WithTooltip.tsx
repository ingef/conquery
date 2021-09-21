import styled from "@emotion/styled";
import React, { FC, ReactElement } from "react";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

const tooltipStyles = {
  ul: {
    paddingLeft: 0,
  },
};

interface PropsT {
  className?: string;
  place?: "bottom" | "left" | "right" | "top";
  text?: string;
  html?: ReactElement;
  lazy?: boolean;
}

const WithTooltip: FC<PropsT> = ({
  className,
  children,
  place,
  text,
  html,
  lazy,
}) => {
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
      title={text}
      html={html}
      theme="light"
      style={tooltipStyles}
      {...delayProps}
    >
      {children}
    </Tooltip>
  );
};

export default WithTooltip;
