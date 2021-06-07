import styled from "@emotion/styled";
import React, { FC } from "react";

import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import { tableHasActiveFilters, tableIsDisabled } from "../model/table";
import type { TableWithFilterValueT } from "../standard-query-editor/types";

const MenuColumnButton = styled("div")<{ disabled?: boolean }>`
  font-size: ${({ theme }) => theme.font.md};
  line-height: 21px;
  padding: 8px 15px;
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

const SxIconButton = styled(IconButton)`
  font-size: ${({ theme }) => theme.font.lg};
  line-height: ${({ theme }) => theme.font.lg};
  padding: 0;

  svg {
    font-size: ${({ theme }) => theme.font.lg};
    line-height: ${({ theme }) => theme.font.lg};
  }
`;
const SxFaIcon = styled(FaIcon)`
  font-size: ${({ theme }) => theme.font.lg};
  line-height: ${({ theme }) => theme.font.lg};
`;

const Label = styled("span")`
  padding-left: 10px;
  line-height: ${({ theme }) => theme.font.lg};
`;

interface PropsT {
  table: TableWithFilterValueT;
  isActive: boolean;
  isOnlyOneTableIncluded: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onClick: () => void;
  onToggleTable: (value: boolean) => void;
}

const MenuColumnItem: FC<PropsT> = ({
  table,
  isActive,
  isOnlyOneTableIncluded,
  blocklistedTables,
  allowlistedTables,
  onClick,
  onToggleTable,
}) => {
  const isDisabled = tableIsDisabled(
    table,
    blocklistedTables,
    allowlistedTables,
  );

  const includable = table.exclude;
  const excludable = !isOnlyOneTableIncluded;

  const isFilterActive = tableHasActiveFilters(table);

  return (
    <MenuColumnButton disabled={isDisabled} onClick={onClick}>
      <SxIconButton
        regular
        icon={includable ? "square" : "check-square"}
        disabled={isDisabled || (!includable && !excludable)}
        onClick={(event) => {
          // To prevent selecting the table as well, see above
          event.stopPropagation();

          if (isDisabled) {
            return;
          }

          if (includable || excludable) {
            onToggleTable(!table.exclude);
          }
        }}
      />
      <Label>{table.label}</Label>
      {isFilterActive && (
        <SxFaIcon right white={isActive} light={!isActive} icon="filter" />
      )}
    </MenuColumnButton>
  );
};

export default MenuColumnItem;
