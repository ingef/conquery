import React, { FC } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import TableFilters from "./TableFilters";
import TableSelects from "./TableSelects";
import ContentCell from "./ContentCell";
import DateColumnSelect from "./DateColumnSelect";
import type { QueryNodeEditorPropsT } from "./QueryNodeEditor";

const Column = styled("div")`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`;

const MaximizedCell = styled(ContentCell)`
  flex-grow: 1;
  padding-bottom: 30px;
`;

const TableView: FC<QueryNodeEditorPropsT> = ({
  node,
  editorState,
  datasetId,
  suggestions,
  currencyConfig,

  onSelectTableSelects,
  onSetDateColumn,

  onSetFilterValue,
  onSwitchFilterMode,
  onLoadFilterSuggestions,
}) => {
  const { t } = useTranslation();

  const table = node.tables[editorState.selectedInputTableIdx];

  const displaySelects = !!table.selects && table.selects.length > 0;
  const displayDateColumnOptions =
    !!table.dateColumn && table.dateColumn.options.length > 0;
  const displayFilters = !!table.filters && table.filters.length > 0;

  return (
    <Column>
      {displaySelects && (
        <ContentCell headline={t("queryNodeEditor.selects")}>
          <TableSelects
            selects={table.selects}
            onSelectTableSelects={(value) =>
              onSelectTableSelects(editorState.selectedInputTableIdx, value)
            }
          />
        </ContentCell>
      )}
      {displayDateColumnOptions && (
        <ContentCell headline={t("queryNodeEditor.selectValidityDate")}>
          <DateColumnSelect
            dateColumn={table.dateColumn}
            onSelectDateColumn={(value) =>
              onSetDateColumn(editorState.selectedInputTableIdx, value)
            }
          />
        </ContentCell>
      )}
      {displayFilters && (
        <MaximizedCell headline={t("queryNodeEditor.filters")}>
          <TableFilters
            key={editorState.selectedInputTableIdx}
            filters={table.filters}
            context={{
              datasetId,
              treeId: node.tree,
              tableId: table.id,
            }}
            onSetFilterValue={(filterIdx: number, value: unknown) =>
              onSetFilterValue(
                editorState.selectedInputTableIdx,
                filterIdx,
                value
              )
            }
            onSwitchFilterMode={(filterIdx, mode) =>
              onSwitchFilterMode(
                editorState.selectedInputTableIdx,
                filterIdx,
                mode
              )
            }
            onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
              onLoadFilterSuggestions(
                datasetId,
                node.tree,
                table.id,
                filterId,
                prefix,
                editorState.selectedInputTableIdx,
                filterIdx
              )
            }
            suggestions={
              !!suggestions && suggestions[editorState.selectedInputTableIdx]
            }
            onShowDescription={editorState.onShowDescription}
            currencyConfig={currencyConfig}
          />
        </MaximizedCell>
      )}
    </Column>
  );
};

export default TableView;
