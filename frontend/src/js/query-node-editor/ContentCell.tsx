import React, { FC } from "react";
import styled from "@emotion/styled";
import { Heading5 } from "../headings/Headings";

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

const Headline = styled(Heading5)`
  margin: 10px 0 0;
`;

interface PropsT {
  className?: string;
  headline?: string;
}

const ContentCell: FC<PropsT> = ({ className, headline, children }) => (
  <Root className={className}>
    {headline && <Headline>{headline}</Headline>}
    <Content>{children}</Content>
  </Root>
);

export default ContentCell;
