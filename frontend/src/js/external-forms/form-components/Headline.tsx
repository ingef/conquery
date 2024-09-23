import { css } from "@emotion/react";
import styled from "@emotion/styled";

import { Headline as HeadlineField } from "../config-types";

const HEADLINE_DOM = {
  h1: "h3" as const,
  h2: "h4" as const,
  h3: "h5" as const,
};

export const getHeadlineFieldAs = (headline: HeadlineField) => {
  if (!headline.style?.size) return "h3";

  // To convert the "simplified" headline type to the real DOM element type
  return HEADLINE_DOM[headline.style.size];
};

export const Headline = styled("h3")<{ size?: "h1" | "h2" | "h3" }>`
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: ${({ theme, size }) =>
    size === "h3"
      ? theme.font.sm
      : size === "h2"
      ? theme.font.md
      : theme.font.lg};
  line-height: 1;
  color: ${({ theme }) => theme.col.black};
  font-weight: ${({ size }) => (size === "h3" ? "700" : "400")};

  &:first-child {
    margin-top: 0;
  }

  position: relative;

  ${({ size }) =>
    (!size || size === "h1") &&
    css`
      margin: 20px 0 5px;
      margin-left: 0;
    `};

  ${({ size }) =>
    (size === "h2" || size === "h3") &&
    css`
      border-left: 0;
      padding-left: 0;
      margin: 10px 0 3px;
      margin-left: 10px;
    `};
`;

export const HeadlineIndex = styled("span")`
  padding: 0 10px;
  font-size: ${({ theme }) => theme.font.lg};
  border-right: 3px solid ${({ theme }) => theme.col.grayMediumLight};
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.col.grayMediumLight};
`;
