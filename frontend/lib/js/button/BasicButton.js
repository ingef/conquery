import React from "react";
import styled from "@emotion/styled";

const BasicButton = styled("button")`
  cursor: pointer;
  font-weight: ${({ active }) => (active ? 700 : 400)};
  padding: ${({ small, tiny, bare }) =>
    bare ? "0" : tiny ? "4px 6px" : small ? "6px 10px" : "8px 15px"};
  font-size: ${({ theme, small, tiny }) =>
    tiny || (small && theme.font.xs) || theme.font.sm};
  transition: all 0.2s;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
`;

export default ({ children, ...props }) => (
  <BasicButton type="button" {...props}>
    {children}
  </BasicButton>
);
