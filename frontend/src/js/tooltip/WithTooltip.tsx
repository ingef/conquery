import React, { FC, ReactElement } from "react";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

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

  return (
    <Tooltip
      className={className}
      position={place || "top"}
      arrow={true}
      duration={0}
      delay={lazy ? 1000 : 0}
      hideDelay={0}
      hideDuration={0}
      title={text}
      html={html}
      theme="light"
      interactive
    >
      {children}
    </Tooltip>
  );
};

export default WithTooltip;
