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

const Headline = styled("h5")`
  margin: 10px 18px 0;
  font-size: ${({ theme }) => theme.font.sm};
`;

type PropsType = {
  className?: string;
  children?: React.Node;
  headline?: string;
};

export default ({ className, headline, children }: PropsType) => (
  <Root className={className}>
    {headline && <Headline>{headline}</Headline>}
    <Content>{children}</Content>
  </Root>
);
