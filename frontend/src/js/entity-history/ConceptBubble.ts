import styled from "@emotion/styled";

export const ConceptBubble = styled("span")`
  padding: 0 3px;
  border-radius: ${({ theme }) => theme.borderRadius};
  color: ${({ theme }) => theme.col.black};
  border: 1px solid ${({ theme }) => theme.col.gray};
  background-color: white;
  font-size: ${({ theme }) => theme.font.sm};
`;
