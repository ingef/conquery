import styled from "@emotion/styled";

import Header from "../header/Header";
import SnackMessage from "../snack-message/SnackMessage";
import { useStartup } from "../startup/useStartup";

import Content, { ContentPropsT } from "./Content";

const Root = styled("div")`
  height: 100vh;
  width: 100%;
  position: relative;
`;

const App = (props: ContentPropsT) => {
  useStartup();

  return (
    <Root>
      <Header />
      <Content {...props} />
      <SnackMessage />
    </Root>
  );
};

export default App;
