import styled from "@emotion/styled";

export const IndexPrefix = styled("span")`
  flex-shrink: 0;
  display: inline-block;
  margin-right: 7px;
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
  padding: 3px;
  line-height: 1;
  font-size: ${({ theme }) => theme.font.tiny};
`;
