// @flow

import React from "react";
import T from "i18n-react";
import styled from "@emotion/styled";

import { tableHasActiveFilters } from "../model/table";

import TransparentHeaderButton from "../button/TransparentHeaderButton";
import FaIcon from "../icon/FaIcon";

import ResetAllFiltersButton from "./ResetAllFiltersButton";
import type { PropsType } from "./QueryNodeEditor";

const FixedColumn = styled("div")`
  height: 100%;
  display: flex;
  flex-direction: column;
  max-width: 220px;
  flex-shrink: 0;
  flex-grow: 1;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

const CategoryHeader = styled("div")`
  padding-left: 10px;
  line-height: 32px;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.black};
`;

const StyledButton = styled(TransparentHeaderButton)`
  font-size: ${({ theme }) => theme.font.lg};
  font-weight: 700;
  color: ${({ theme }) => theme.col.black};
  width: 100%;
  text-align: left;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  line-height: 21px;

  background-color: ${({ theme, active }) =>
    active ? theme.col.blueGrayVeryLight : "initial"};
  &:hover {
    background-color: ${({ theme, active }) =>
      active ? theme.col.blueGrayVeryLight : "initial"};
  }
`;

const StyledFaIcon = styled(FaIcon)`
  font-size: 21px;
  line-height: 21px;
`;

const MenuColumn = (props: PropsType) => {
  const {
    node,
    editorState,
    showTables,
    onToggleTable,
    onResetAllFilters
  } = props;

  const onlyOneTableIncluded = !node.isPreviousQuery
    ? node.tables.filter(table => !table.exclude).length === 1
    : undefined;
  const allowToggleTables = !node.isPreviousQuery
    ? node.tables.map(table => table.exclude || !onlyOneTableIncluded)
    : undefined;

  return (
    <FixedColumn>
      <CategoryHeader>
        {T.translate("queryNodeEditor.queryNode")}
      </CategoryHeader>
      <StyledButton
        active={editorState.detailsViewActive}
        onClick={e => {
          e.preventDefault();
          editorState.onSelectDetailsView();
        }}
      >
        {T.translate("queryNodeEditor.properties")}
      </StyledButton>
      {!node.isPreviousQuery && showTables && (
        <div>
          <CategoryHeader>
            {T.translate("queryNodeEditor.conceptNodeTables")}
          </CategoryHeader>
          {node.tables.map((table, tableIdx) => {
            const isActive =
              editorState.selectedInputTableIdx === tableIdx &&
              !editorState.detailsViewActive;

            return (
              <StyledButton
                key={tableIdx}
                active={isActive}
                onClick={() => {
                  editorState.onSelectInputTableView(tableIdx);
                }}
              >
                <StyledFaIcon
                  left
                  icon={table.exclude ? "square-o" : "check-square-o"}
                  disabled={!allowToggleTables[tableIdx]}
                  onClick={event => {
                    event.stopPropagation();

                    if (allowToggleTables[tableIdx])
                      onToggleTable(tableIdx, !table.exclude);
                  }}
                />
                {table.label}
                {tableHasActiveFilters(table) && (
                  <StyledFaIcon
                    right
                    white={isActive}
                    light={!isActive}
                    icon="filter"
                  />
                )}
              </StyledButton>
            );
          })}
          <ResetAllFiltersButton
            node={node}
            onResetAllFilters={onResetAllFilters}
          />
        </div>
      )}
    </FixedColumn>
  );
};

export default MenuColumn;
