import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "@emotion/styled";

import EditableText from "../form-components/EditableText";

import ResetAllFiltersButton from "./ResetAllFiltersButton";
import MenuColumnItem from "./MenuColumnItem";
import MenuColumnButton from "./MenuColumnButton";

import type { QueryNodeEditorPropsT } from "./QueryNodeEditor";

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

const NodeName = styled("div")`
  padding: 10px 15px;
  border-bottom: 1px solid #ccc;
`;

const MenuColumn: FC<QueryNodeEditorPropsT> = ({
  node,
  editorState,
  showTables,
  blocklistedTables,
  allowlistedTables,
  onToggleTable,
  onResetAllFilters,
  onUpdateLabel,
}) => {
  const { t } = useTranslation();
  const isOnlyOneTableIncluded =
    !node.isPreviousQuery &&
    node.tables.filter((table) => !table.exclude).length === 1;

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
            onSubmit={(value) => {
              onUpdateLabel(value);
              editorState.onToggleEditLabel();
            }}
            onToggleEdit={editorState.onToggleEditLabel}
          />
        )}
        {node.isPreviousQuery && (node.label || node.id || node.ids)}
      </NodeName>
      <MenuColumnButton
        active={editorState.detailsViewActive}
        onClick={editorState.onSelectDetailsView}
      >
        {t("queryNodeEditor.properties")}
      </MenuColumnButton>
      {!node.isPreviousQuery && showTables && (
        <div>
          <CategoryHeader>
            {t("queryNodeEditor.conceptNodeTables")}
          </CategoryHeader>
          {node.tables.map((table, tableIdx) => (
            <MenuColumnItem
              key={tableIdx}
              table={table}
              isActive={
                editorState.selectedInputTableIdx === tableIdx &&
                !editorState.detailsViewActive
              }
              isOnlyOneTableIncluded={isOnlyOneTableIncluded}
              blocklistedTables={blocklistedTables}
              allowlistedTables={allowlistedTables}
              onClick={() => editorState.onSelectInputTableView(tableIdx)}
              onToggleTable={(value) => onToggleTable(tableIdx, value)}
            />
          ))}
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
