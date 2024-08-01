import styled from "@emotion/styled";

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

interface Props {
  tabs: TabNavigationTab[];
  right?: boolean;
  left?: boolean;
  className?: string;
  dataTestId: string;
  children: React.ReactNode;
}

const Pane = ({ tabs, left, children, className, dataTestId }: Props) => {
  const paneType = left ? "left" : "right";

  return (
    <Root className={className}>
      <Container>
        <PaneTabNavigation
          tabs={tabs}
          paneType={paneType}
          dataTestId={dataTestId}
        />
        <Container data-test-id={dataTestId + "-container"}>
          {children}
        </Container>
      </Container>
    </Root>
  );
};

export default Pane;
