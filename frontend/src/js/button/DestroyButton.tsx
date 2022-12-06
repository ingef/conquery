import styled from "@emotion/styled";

import { TransparentButton } from "./TransparentButton";

export const DestroyButton = styled(TransparentButton)`
  color: ${({ theme }) => theme.col.red};
  border: 2px solid ${({ theme }) => theme.col.red};

  &:hover,
  &:active,
  &:focus {
    color: white;
    background-color: ${({ theme }) => theme.col.red};
    border: 2px solid ${({ theme }) => theme.col.red};
  }
`;
