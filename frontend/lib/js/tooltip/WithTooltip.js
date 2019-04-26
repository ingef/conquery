// @flow

import * as React from "react";
import styled from "@emotion/styled";
import { Tooltip } from "react-tippy";
import "react-tippy/dist/tippy.css";

type PropsType = {
  className?: string,
  children: React.Node,
  place?: string,
  text?: string
};

const WithTooltip = ({ className, children, place, text }: PropsType) => {
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
