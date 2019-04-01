// @flow

import * as React from "react";
import ReactTooltip from "react-tooltip";
import styled from "@emotion/styled";

type PropsType = {
  className?: string,
  children: React.Node,
  place?: string
};

const Root = styled("div")`
  div[data-id="tooltip"] {
    text-transform: initial;
  }
`;

const WithTooltip = ({ className, children, place }: PropsType) => {
  return (
    <Root className={className}>
      {children}
      <ReactTooltip
        place={place || "top"}
        type="info"
        effect="solid"
        multiline={true}
      />
    </Root>
  );
};

export default WithTooltip;
