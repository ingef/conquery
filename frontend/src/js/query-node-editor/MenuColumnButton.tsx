import styled from "@emotion/styled";

import BasicButton from "../button/BasicButton";

const MenuColumnButton = styled(BasicButton)`
  font-size: ${({ theme }) => theme.font.md};
  line-height: 21px;
  border: 0;
  border-radius: 0;
  font-weight: 700;
  color: ${({ theme, disabled }) =>
    disabled ? theme.col.gray : theme.col.black};
  width: 100%;
  text-align: left;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  background-color: transparent;

  &:hover {
    text-decoration: underline;
  }
`;

export default MenuColumnButton;
