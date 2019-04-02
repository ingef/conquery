// @flow

import * as React from "react";
import styled from "@emotion/styled";

const Root = styled("div")`
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-width: 220px;
`;

const Content = styled("div")`
  flex-grow: 1;
  padding: 10px;
`;

type PropsType = {
  className?: string,
  children?: React.Node
};

export default ({ className, headline, children }: PropsType) => (
  <Root className={className}>
    <Content>{children}</Content>
  </Root>
);
