import styled from "@emotion/styled";
import { ReactNode } from "react";

const KeyShape = styled("kbd")`
  padding: 2px 4px;
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  box-shadow: 0 0 3px 0 ${({ theme }) => theme.col.grayLight};
  font-size: ${({ theme }) => theme.font.xs};
  line-height: 1;
  border-radius: ${({ theme }) => theme.borderRadius};
  text-transform: uppercase;
`;

export const KeyboardKey = ({ children }: { children: ReactNode }) => (
  <KeyShape>{children}</KeyShape>
);
