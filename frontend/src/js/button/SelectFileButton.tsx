import styled from "@emotion/styled";

import BasicButton from "./BasicButton";

export const SelectFileButton = styled(BasicButton)`
  color: ${({ theme }) => theme.col.gray};
  background-color: transparent;
  font-weight: 300;
  border: none;
  font-size: ${({ theme }) => theme.font.tiny};
  display: flex;
  align-items: center;
  gap: 5px;

  &:hover {
    text-decoration: underline;
  }
`;
