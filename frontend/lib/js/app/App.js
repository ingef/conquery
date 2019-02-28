// @flow

import React from "react";
import { Startup } from "../startup";
import { Header } from "../header";
import Content from "./Content";

const App = () => (
  <div className="app">
    <Startup />
    <Header />
    <Content />
  </div>
);

export default App;
