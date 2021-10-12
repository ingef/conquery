import styled from "@emotion/styled";
import React from "react";

import BasicButton from "./BasicButton";

export const TransparentButton = styled(BasicButton)<{ light?: boolean }>`
  color: ${({ theme, light }) => (light ? theme.col.gray : theme.col.black)};
  background-color: transparent;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid
    ${({ theme, light }) => (light ? theme.col.grayLight : theme.col.gray)};

  &:hover {
    background-color: ${({ theme }) => theme.col.grayVeryLight};
  }

  &:focus {
    border: 1px solid ${({ theme }) => theme.col.green};
    background-color: ${({ theme }) => theme.col.grayVeryLight};
  }
`;
