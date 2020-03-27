import styled from "@emotion/styled";

const Label = styled("span")`
  font-size: ${({ theme, tiny, large }) =>
    large ? theme.font.md : tiny ? theme.font.xs : theme.font.sm};
  display: ${({ inline }) => (inline ? "inline-block" : "block")};
  margin: ${({ inline }) => (inline ? "0" : "2px 8px")};
  color: ${({ theme, disabled }) => (disabled ? theme.col.gray : "initial")};
  width: ${({ fullWidth }) => (fullWidth ? "100%" : "inherit")};
`;

export default Label;
