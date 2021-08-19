import styled from "@emotion/styled";

const Label = styled("span")<{
  tiny?: boolean;
  large?: boolean;
  disabled?: boolean;
  fullWidth?: boolean;
}>`
  font-size: ${({ theme, tiny, large }) =>
    large ? theme.font.md : tiny ? theme.font.xs : theme.font.sm};
  display: flex;
  align-items: center;
  margin: 6px 0 3px;
  color: ${({ theme, disabled }) => (disabled ? theme.col.gray : "initial")};
  width: ${({ fullWidth }) => (fullWidth ? "100%" : "inherit")};
`;

export default Label;
