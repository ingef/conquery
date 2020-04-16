import React from "react";
import styled from "@emotion/styled";
import { Dot } from "react-animated-dots";

const AnimatedDots = styled("span")`
  font-weight: 700;
`;

export default () => (
  <AnimatedDots>
    <Dot>.</Dot>
    <Dot>.</Dot>
    <Dot>.</Dot>
  </AnimatedDots>
);
