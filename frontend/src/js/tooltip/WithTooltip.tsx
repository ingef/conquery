import * as React from "react";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

type PropsType = {
  className?: string;
  children: React.ReacNode;
  place?: string;
  text?: string | null;
};

const WithTooltip = ({ className, children, place, text }: PropsType) => {
  if (!text) return children;

  return (
    <Tooltip
      className={className}
      position={place || "top"}
      arrow={true}
      duration={0}
      delay={[0, 0]}
      title={text}
    >
      {children}
    </Tooltip>
  );
};

export default WithTooltip;
