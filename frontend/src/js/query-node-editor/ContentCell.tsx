import styled from "@emotion/styled";
import React, { FC, forwardRef, ReactNode } from "react";

import { Heading4 } from "../headings/Headings";

const Root = styled("div")`
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-width: 220px;
`;

const Content = styled("div")`
  flex-grow: 1;
  padding: 3px 10px;
`;

const Headline = styled(Heading4)`
  margin: 14px 10px 0;
`;

interface PropsT {
  className?: string;
  children?: ReactNode;
  headline?: string;
}

const ContentCell = forwardRef<HTMLDivElement, PropsT>(
  ({ className, headline, children }, ref) => (
    <Root ref={ref} className={className}>
      {headline && <Headline>{headline}</Headline>}
      <Content>{children}</Content>
    </Root>
  ),
);

export default ContentCell;
