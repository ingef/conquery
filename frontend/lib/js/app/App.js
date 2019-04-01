// @flow

import React from "react";
import styled from "@emotion/styled";

import { Startup } from "../startup";
import { Header } from "../header";
import Content from "./Content";

const Root = styled("div")`
  height: 100vh;
  width: 100%;
  position: relative;
`;

const App = () => (
  <Root>
    <Startup />
    <Header />
    <Content />
  </Root>
);

export default App;
