import styled from "@emotion/styled";

import BasicButton from "../../button/BasicButton";
import { List } from "../InputSelect/InputSelectComponents";

export const Root = styled("div")`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const SelectMenuContainer = styled("div")`
  position: absolute;
  top: 40px;
  left: 0;
  right: 0;
`;

export const OptionList = styled(List)`
  display: grid;
  grid-template-columns: auto auto;
  gap: 5px;
`;

export const OptionButton = styled(BasicButton)`
  font-size: 14px;
  background: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : "inherit"};
  color: ${({ active }) => (active ? "white" : "inherit")};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: ${({ theme }) => "1px solid " + theme.col.gray};
  &:hover {
    background: ${({ theme }) => theme.col.blueGrayDark};
    color: white;
  }
`;

export const MonthYearLabel = styled("div")`
  font-weight: bold;
  cursor: pointer;
`;
