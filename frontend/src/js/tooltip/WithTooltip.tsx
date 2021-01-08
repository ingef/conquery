import React, { FC, ReactElement } from "react";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

interface PropsT {
  className?: string;
  place?: "bottom" | "left" | "right" | "top";
  text?: string;
  html?: ReactElement;
}

const WithTooltip: FC<PropsT> = ({
  className,
  children,
  place,
  text,
  html,
}) => {
  if (!text && !html) return <>{children}</>;

  return (
    <Tooltip
      className={className}
      position={place || "top"}
      arrow={true}
      duration={0}
      delay={0}
      title={text}
      html={html}
    >
      {children}
    </Tooltip>
  );
};

export default WithTooltip;
