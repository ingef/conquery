import styled from "@emotion/styled";
import { FC } from "react";

import PaneTabNavigation from "./PaneTabNavigation";
import { TabNavigationTab } from "./TabNavigation";

const Root = styled("div")<{ left?: boolean; right?: boolean }>`
  width: 100%;
  height: 100%;

  padding: ${({ left, right }) => (left || right ? "40px 0 10px" : "0")};
`;

const Container = styled("div")`
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
`;

interface PropsT {
  tabs: TabNavigationTab[];
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
