import * as React from "react";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

interface PropsT {
  className?: string;
  place?: string;
  text?: React.ReactNode;
}

const WithTooltip: React.FC<PropsT> = ({
  className,
  children,
  place,
  text
}) => {
  if (!text) return <>{children}</>;

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
