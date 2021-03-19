import React, { FC } from "react";
import styled from "@emotion/styled";
import PaneTabNavigation from "./PaneTabNavigation";

const Root = styled("div")<{ left?: boolean; right?: boolean }>`
  width: 100%;
  height: 100%;

  padding: ${({ left, right }) => (left || right ? "50px 0 10px" : "0")};
`;

const Container = styled("div")`
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
`;

interface PropsT {
  tabs: { key: string; label: string }[];
  right?: boolean;
  left?: boolean;
}

const Pane: FC<PropsT> = ({ tabs, left, right, children }) => {
  const paneType = left ? "left" : "right";

  return (
    <Root left={left} right={right}>
      <Container>
        <PaneTabNavigation tabs={tabs} paneType={paneType} />
        <Container>{children}</Container>
      </Container>
    </Root>
  );
};

export default Pane;
