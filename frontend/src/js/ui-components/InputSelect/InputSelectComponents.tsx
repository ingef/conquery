import { css } from "@emotion/react";
import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";
import SelectListOption from "../SelectListOption";

export const Control = styled("div")<{ disabled?: boolean }>`
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: 4px;
  display: flex;
  align-items: flex-start;
  overflow: hidden;
  padding: 4px 3px 4px 8px;
  background-color: white;
  ${({ disabled }) =>
    disabled &&
    css`
      cursor: not-allowed;
    `}

  &:focus {
    outline: 1px solid black;
  }
`;

export const SelectContainer = styled("div")`
  width: 100%;
  position: relative;
`;

export const ItemsInputContainer = styled("div")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  width: 100%;
`;

export const Menu = styled("div")`
  position: absolute;
  width: 100%;
  border-radius: 4px;
  box-shadow: 0 0 0 1px hsl(0deg 0% 0% / 10%), 0 4px 11px hsl(0deg 0% 0% / 10%);
  background-color: ${({ theme }) => theme.col.bg};
  z-index: 2;
`;

export const List = styled("div")`
  padding: 3px;
  max-height: 304px; /* matches 11 items including the spacing in between items */
  overflow-y: auto;
  --webkit-overflow-scrolling: touch;
`;

export const Input = styled("input")`
  border: 0;
  height: 20px;
  outline: none;
  flex-grow: 1;
  width: 0; /* to fix default width */
  ${({ disabled }) =>
    disabled &&
    css`
      cursor: not-allowed;
      pointer-events: none;
      &:placehoder {
        opacity: 0.5;
      }
    `}
`;

export const DropdownToggleButton = styled(IconButton)`
  padding: 2px 4px 2px 6px;
`;

export const ResetButton = styled(IconButton)`
  padding: 2px 8px;
`;

export const VerticalSeparator = styled("div")`
  width: 1px;
  margin: 3px 0;
  background-color: ${({ theme }) => theme.col.grayLight};
  align-self: stretch;
  flex-shrink: 0;
`;

export const SxSelectListOption = styled(SelectListOption)`
  margin-bottom: 2px;
`;
