// @flow

import React from "react";
import T from "i18n-react";
import styled from "@emotion/styled";

import { tableHasActiveFilters, tableIsDisabled } from "../model/table";

import { EditableText } from "../form-components";
import FaIcon from "../icon/FaIcon";
import BasicButton from "../button/BasicButton";

import ResetAllFiltersButton from "./ResetAllFiltersButton";
import type { PropsType } from "./QueryNodeEditor";

const FixedColumn = styled("div")`
  height: 100%;
  display: flex;
  flex-direction: column;
  min-width: 205px;
  max-width: 220px;
  overflow: hidden;
  flex-shrink: 0;
  flex-grow: 1;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

const CategoryHeader = styled("p")`
  margin: 0;
  padding: 10px 0 5px 14px;
  line-height: 1;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.black};
`;

const StyledButton = styled(BasicButton)`
  font-size: ${({ theme }) => theme.font.md};
  line-height: 21px;
  border: 0;
  margin-top: 3px;
  font-weight: 700;
  color: ${({ theme, disabled }) =>
    disabled ? theme.col.gray : theme.col.black};
  width: 100%;
  text-align: left;
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  transition: background-color 0.1s;

  background-color: ${({ theme, active, disabled }) =>
    active
      ? theme.col.blueGrayVeryLight
      : disabled
      ? "transparent"
      : theme.col.grayVeryLight};
  &:hover {
    background-color: ${({ theme, active, disabled }) =>
      active
        ? theme.col.blueGrayVeryLight
        : disabled
        ? "transparent"
        : theme.col.grayLight};
  }
`;

const StyledFaIcon = styled(FaIcon)`
  font-size: ${({ theme }) => theme.font.lg};
  line-height: ${({ theme }) => theme.font.lg};
`;

const NodeName = styled("div")`
  padding: 10px 15px;
  border-bottom: 1px solid #ccc;
`;

const MenuColumn = (props: PropsType) => {
  const {
    node,
    editorState,
    showTables,
    disabledTables,
    onToggleTable,
    onResetAllFilters,
    onUpdateLabel
  } = props;

  const onlyOneTableIncluded =
    !node.isPreviousQuery &&
    node.tables.filter(table => !table.exclude).length === 1;

  return (
    <FixedColumn>
      <NodeName>
        {!node.isPreviousQuery && (
          <EditableText
            large
            loading={false}
            text={node.label}
            selectTextOnMount={true}
            editing={editorState.editingLabel}
            onSubmit={value => {
              onUpdateLabel(value);
              editorState.onToggleEditLabel();
            }}
            onToggleEdit={editorState.onToggleEditLabel}
          />
        )}
        {node.isPreviousQuery && (node.label || node.id || node.ids)}
      </NodeName>
      <StyledButton
        active={editorState.detailsViewActive}
        onClick={editorState.onSelectDetailsView}
      >
        {T.translate("queryNodeEditor.properties")}
      </StyledButton>
      {!node.isPreviousQuery && showTables && (
        <div>
          <CategoryHeader>
            {T.translate("queryNodeEditor.conceptNodeTables")}
          </CategoryHeader>
          {node.tables.map((table, tableIdx) => {
            const isDisabled = tableIsDisabled(table, disabledTables);
            const isActive =
              editorState.selectedInputTableIdx === tableIdx &&
              !editorState.detailsViewActive;

            return (
              <StyledButton
                key={tableIdx}
                active={isActive}
                disabled={isDisabled}
                onClick={() => {
                  editorState.onSelectInputTableView(tableIdx);
                }}
              >
                <StyledFaIcon
                  left
                  regular
                  icon={table.exclude ? "square" : "check-square"}
                  disabled={
                    isDisabled || (!table.exclude && onlyOneTableIncluded)
                  }
                  onClick={event => {
                    event.stopPropagation();

                    if (!isDisabled && (table.exclude || !onlyOneTableIncluded))
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
