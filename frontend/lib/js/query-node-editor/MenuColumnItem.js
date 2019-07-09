// @flow

import React from "react";
import T from "i18n-react";
import styled from "@emotion/styled";

import FaIcon from "../icon/FaIcon";
import { tableHasActiveFilters, tableIsDisabled } from "../model/table";
import type { TableWithFilterValueType } from "../standard-query-node-editor/types";

import MenuColumnButton from "./MenuColumnButton";

const StyledFaIcon = styled(FaIcon)`
  font-size: ${({ theme }) => theme.font.lg};
  line-height: ${({ theme }) => theme.font.lg};
`;

type PropsT = {
  table: TableWithFilterValueType,
  isActive: boolean,
  isOnlyOneTableIncluded: boolean,
  blacklistedTables?: string[],
  whitelistedTables?: string[],
  onClick: () => void,
  onToggleTable: (value: boolean) => void
};

export default ({
  table,
  isActive,
  isOnlyOneTableIncluded,
  blacklistedTables,
  whitelistedTables,
  onClick,
  onToggleTable
}: PropsT) => {
  const isDisabled = tableIsDisabled(
    table,
    blacklistedTables,
    whitelistedTables
  );

  return (
    <MenuColumnButton active={isActive} disabled={isDisabled} onClick={onClick}>
      <StyledFaIcon
        left
        regular
        icon={table.exclude ? "square" : "check-square"}
        disabled={isDisabled || (!table.exclude && isOnlyOneTableIncluded)}
        onClick={event => {
          // To prevent selecting the table as well, see above
          event.stopPropagation();

          if (!isDisabled && (table.exclude || !isOnlyOneTableIncluded))
            onToggleTable(!table.exclude);
        }}
      />
      {table.label}
      {tableHasActiveFilters(table) && (
        <StyledFaIcon right white={isActive} light={!isActive} icon="filter" />
      )}
    </MenuColumnButton>
  );
};
