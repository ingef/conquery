// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import TableFilters from "./TableFilters";
import TableSelects from "./TableSelects";
import ContentCell from "./ContentCell";
import DateColumnSelect from "./DateColumnSelect";
import type { PropsType } from "./QueryNodeEditor";

const Column = styled("div")`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`;

const MaximizedCell = styled(ContentCell)`
  flex-grow: 1;
  padding-bottom: 30px;
`;

const TableView = (props: PropsType) => {
  const {
    node,
    editorState,
    datasetId,

    onSelectTableSelects,
    onSetDateColumn,

    onSetFilterValue,
    onSwitchFilterMode,
    onLoadFilterSuggestions
  } = props;

  const selectedTable = node.tables[editorState.selectedInputTableIdx];

  return (
    <Column>
      {selectedTable.selects && (
        <ContentCell headline={T.translate("queryNodeEditor.selects")}>
          <TableSelects
            selects={selectedTable.selects}
            onSelectTableSelects={value =>
              onSelectTableSelects(editorState.selectedInputTableIdx, value)
            }
          />
        </ContentCell>
      )}
      {!!selectedTable.dateColumn &&
        !!selectedTable.dateColumn.options &&
        selectedTable.dateColumn.options.length > 0 && (
          <ContentCell
            headline={T.translate("queryNodeEditor.selectValidityDate")}
          >
            <DateColumnSelect
              dateColumn={selectedTable.dateColumn}
              onSelectDateColumn={value =>
                onSetDateColumn(editorState.selectedInputTableIdx, value)
              }
            />
          </ContentCell>
        )}
      <MaximizedCell headline={T.translate("queryNodeEditor.filters")}>
        <TableFilters
          key={editorState.selectedInputTableIdx}
          filters={selectedTable.filters}
          context={{
            datasetId,
            treeId: node.tree,
            tableId: selectedTable.id
          }}
          onSetFilterValue={(filterIdx, value) =>
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
              selectedTable.id,
              filterId,
              prefix,
              editorState.selectedInputTableIdx,
              filterIdx
            )
          }
          suggestions={
            !!props.suggestions &&
            props.suggestions[editorState.selectedInputTableIdx]
          }
          onShowDescription={editorState.onShowDescription}
          currencyConfig={props.currencyConfig}
        />
      </MaximizedCell>
    </Column>
  );
};

export default TableView;
