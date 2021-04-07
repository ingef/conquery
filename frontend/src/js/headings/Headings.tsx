import styled from "app-theme";

export const Heading3 = styled("h3")`
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.black};
`;

export const Heading5 = styled("h5")`
  font-weight: 400;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  text-transform: uppercase;
`;
