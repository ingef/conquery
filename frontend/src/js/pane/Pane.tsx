import styled from "@emotion/styled";
import { FC } from "react";

import PaneTabNavigation from "./PaneTabNavigation";
import { TabNavigationTab } from "./TabNavigation";

const Root = styled("div")`
  width: 100%;
  height: 100%;

  padding: 40px 0 0;
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
  className?: string;
}

const Pane: FC<PropsT> = ({ tabs, left, children, className }) => {
  const paneType = left ? "left" : "right";

  return (
    <Root className={className}>
      <Container>
        <PaneTabNavigation tabs={tabs} paneType={paneType} />
        <Container>{children}</Container>
      </Container>
    </Root>
  );
};

export default Pane;
