import React from "react";
import styled from "@emotion/styled";
import { keyframes } from "@emotion/react";

type PropsType = {
  isQueryRunning: boolean;
};

const spin = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
`;

const Spinner = styled("div")`
  height: 30px;
  width: 30px;
  background-image: url('${({ theme }) => theme.img.spinner}');
  background-repeat:  no-repeat;
  background-size: 30px;

  animation: ${spin} 1s linear 0s infinite
`;

const QueryRunningSpinner = (props: PropsType) =>
  props.isQueryRunning ? <Spinner /> : null;

export default QueryRunningSpinner;
