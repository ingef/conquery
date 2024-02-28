import styled from "@emotion/styled";
import { ButtonHTMLAttributes, forwardRef } from "react";

export interface BasicButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  bare?: boolean;
  tiny?: boolean;
  small?: boolean;
  large?: boolean;
  active?: boolean;
  secondary?: boolean;
  autoFocus?: boolean; // Should actually be within the extends, not sure why I had to declare this.
}

const Button = styled("button")<BasicButtonProps>`
  cursor: pointer;
  font-weight: ${({ active, secondary }) =>
    active || secondary ? "700" : "400"};
  padding: ${({ small, tiny, bare, large }) =>
    bare
      ? "0"
      : tiny
      ? "4px 6px"
      : small
      ? "6px 10px"
      : large
      ? "12px 18px"
      : "8px 15px"};
  font-size: ${({ theme, small, tiny, large }) =>
    tiny || small ? theme.font.xs : large ? theme.font.lg : theme.font.sm};
  transition: all 0.2s;
  border-radius: ${({ theme }) => theme.borderRadius};

  &:disabled {
    cursor: not-allowed;
    opacity: 0.4;
  }
`;

const BasicButton = forwardRef<HTMLButtonElement, BasicButtonProps>(
  ({ children, ...props }, ref) => (
    <Button type="button" {...props} ref={ref}>
      {children}
    </Button>
  ),
);

export default BasicButton;
