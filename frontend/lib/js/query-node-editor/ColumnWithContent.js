// @flow

import * as React from "react";
import styled from "@emotion/styled";

const Headline = styled("h4")`
  margin: 0px 4px;
  padding: 0px 10px;
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  line-height: 37px;
  font-size: ${({ theme }) => theme.font.md};
  font-weight: 700;
  color: ${({ theme }) => theme.col.black};
`;

const LargeColumn = styled("div")`
  height: 100%;

  display: flex;
  flex-direction: column;
  min-width: 200px;
  overflow: auto;
`;

const ColumnContent = styled("div")`
  flex-grow: 1;
  flex-shrink: 1;
  overflow-y: auto;
  padding: 10px;
`;

type PropsType = {
  className?: string,
  headline: React.Node,
  children?: React.Node
};

export default ({ className, headline, children }: PropsType) => (
  <LargeColumn className={className}>
    <Headline>{headline}</Headline>
    <ColumnContent>{children}</ColumnContent>
  </LargeColumn>
);
