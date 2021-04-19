import { css } from "@emotion/react";
import styled from "@emotion/styled";

export const Cell = styled("code")<{ isDates?: boolean; isHeader?: boolean }>`
  padding: 1px 5px;
  font-size: ${({ theme }) => theme.font.xs};
  height: ${({ theme }) => theme.font.xs};
  min-width: ${({ isDates }) => (isDates ? "300px" : "100px")};
  width: ${({ isDates }) => (isDates ? "auto" : "100px")};
  flex-grow: ${({ isDates }) => (isDates ? "1" : "0")};
  flex-shrink: 0;
  background-color: white;
  margin: 0;
  position: relative;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: ${({ isDates }) => (isDates ? "flex" : "block")};
  align-items: center;
  overflow: hidden;

  ${({ isHeader }) =>
    isHeader &&
    css`
      font-weight: 700;
      overflow-wrap: break-word;
      margin: 0 0 5px;
      text-overflow: initial;
      white-space: initial;
      height: initial;
    `};
`;
