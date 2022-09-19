import styled from "@emotion/styled";

export const TinyLabel = styled("p")`
  margin: 5px 0 0;
  font-size: ${({ theme }) => theme.font.tiny};
  font-weight: 400;
  color: ${({ theme }) => theme.col.gray};
  text-transform: uppercase;
  white-space: nowrap;
  line-height: 1;
`;
