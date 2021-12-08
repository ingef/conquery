import styled from "@emotion/styled";

export const Headline = styled("h3")`
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.black};
  margin: 20px 0 5px;
  font-weight: 700;
`;
