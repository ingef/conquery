import React from "react";
import styled from "@emotion/styled";

import { useStartup } from "../startup/useStartup";
import Header from "../header/Header";
import SnackMessage from "../snack-message/SnackMessage";
import { useKeycloak } from "@react-keycloak/web";
import Content, { ContentPropsT } from "./Content";

const Root = styled("div")`
  height: 100vh;
  width: 100%;
  position: relative;
`;

const App = (props: ContentPropsT) => {
  useStartup();

  const { initialized, keycloak } = useKeycloak();

  console.log(initialized, keycloak);

  return (
    <Root>
      <Header />
      <Content {...props} />
      <SnackMessage />
    </Root>
  );
};

export default App;
