import styled from "@emotion/styled";

export const Heading3 = styled("h3")`
  line-height: 1.3;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.black};
`;

export const Heading4 = styled("h4")`
  line-height: 1.2;
  font-weight: 400;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-transform: uppercase;
`;
