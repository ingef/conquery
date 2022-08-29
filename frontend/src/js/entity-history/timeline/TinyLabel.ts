import styled from "@emotion/styled";

export const TinyLabel = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.tiny};
  font-weight: 700;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.gray};
  line-height: 0.9;
`;
