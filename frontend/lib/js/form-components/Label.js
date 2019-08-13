import styled from "@emotion/styled";

const Label = styled("span")`
  font-size: ${({ theme, tiny }) => (tiny ? theme.font.xs : theme.font.sm)};
  display: ${({ inline }) => (inline ? "inline-block" : "block")};
  margin: ${({ inline }) => (inline ? "0" : "2px 8px")};
  color: ${({ theme, disabled }) => (disabled ? theme.col.gray : "initial")};
  width: ${({ fullWidth }) => (fullWidth ? "100%" : "inherit")};
`;

export default Label;
