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
  font-size: ${({ theme, size }) =>
    size === "h3"
      ? theme.font.sm
      : size === "h2"
      ? theme.font.md
      : theme.font.lg};
  color: ${({ theme }) => theme.col.black};
  margin-top: ${({ size }) =>
    size === "h3" ? "14px" : size === "h2" ? "16px" : "20px"};
  margin-bottom: 5px;
  margin-left: 10px;
  font-weight: ${({ size }) => (size === "h3" ? "700" : "400")};
`;
