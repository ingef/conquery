import { keyframes } from "@emotion/react";
import styled from "@emotion/styled";

const spin = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
`;

export const QueryRunningSpinner = styled("div")`
  height: 30px;
  width: 30px;
  background-image: url("${({ theme }) => theme.img.spinner}");
  background-repeat: no-repeat;
  background-size: 30px;

  animation: ${spin} 1s linear 0s infinite;
`;
