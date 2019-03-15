// @flow

import * as React from "react";
import styled from "@emotion/styled";

const Root = styled("div")`
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-width: 200px;
`;

const Headline = styled("h4")`
  margin: 0px 4px;
  padding: 8px 10px;
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  font-size: ${({ theme }) => theme.font.md};
  font-weight: 700;
  color: ${({ theme }) => theme.col.black};
`;

const Content = styled("div")`
  flex-grow: 1;
  padding: 10px;
`;

type PropsType = {
  className?: string,
  headline: React.Node,
  children?: React.Node
};

export default ({ className, headline, children }: PropsType) => (
  <Root className={className}>
    <Headline>{headline}</Headline>
    <Content>{children}</Content>
  </Root>
);
