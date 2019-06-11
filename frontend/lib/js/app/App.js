// @flow

import React from "react";
import styled from "@emotion/styled";

import Startup from "../startup/Startup";
import Header from "../header/Header";
import SnackMessage from "../snack-message/SnackMessage";
import Content from "./Content";

const Root = styled("div")`
  height: 100vh;
  width: 100%;
  position: relative;
`;

const App = props => (
  <Root>
    <Startup />
    <Header />
    <Content {...props} />
    <SnackMessage />
  </Root>
);

export default App;
