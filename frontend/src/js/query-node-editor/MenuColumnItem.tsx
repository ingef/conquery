import React from "react";
import styled from "@emotion/styled";

import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";

import { tableHasActiveFilters, tableIsDisabled } from "../model/table";
import type { TableWithFilterValueT } from "../standard-query-node-editor/types";

import MenuColumnButton from "./MenuColumnButton";

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

type PropsT = {
  table: TableWithFilterValueT;
  isActive: boolean;
  isOnlyOneTableIncluded: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onClick: () => void;
  onToggleTable: (value: boolean) => void;
};

export default ({
  table,
  isActive,
  isOnlyOneTableIncluded,
  blocklistedTables,
  allowlistedTables,
  onClick,
  onToggleTable,
}: PropsT) => {
  const isDisabled = tableIsDisabled(
    table,
    blocklistedTables,
    allowlistedTables
  );

  // TODO: This creates an invalid DOM nesting, a <button> inside a <button>
  //       Yet, this is the way we can get it to work in IE11
  //       => Try to use a clickable div and a nested button instead
  return (
    <MenuColumnButton active={isActive} disabled={isDisabled} onClick={onClick}>
      <SxIconButton
        regular
        icon={table.exclude ? "square" : "check-square"}
        disabled={isDisabled || (!table.exclude && isOnlyOneTableIncluded)}
        onClick={(event) => {
          // To prevent selecting the table as well, see above
          event.stopPropagation();

          if (!isDisabled && (table.exclude || !isOnlyOneTableIncluded))
            onToggleTable(!table.exclude);
        }}
      />
      <Label>{table.label}</Label>
      {tableHasActiveFilters(table) && (
        <SxFaIcon right white={isActive} light={!isActive} icon="filter" />
      )}
    </MenuColumnButton>
  );
};
