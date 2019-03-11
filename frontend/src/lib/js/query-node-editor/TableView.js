// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import TableFilters from "./TableFilters";
import TableSelects from "./TableSelects";
import ContentCell from "./ContentCell";
import type { PropsType } from "./QueryNodeEditor";

const Column = styled("div")`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`;

const MaximizedCell = styled(ContentCell)`
  flex-grow: 1;
`;

const TableView = (props: PropsType) => {
  const {
    node,
    editorState,
    datasetId,

    onSelectTableSelects,

    onDropFilterValuesFile,
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
      <MaximizedCell headline={T.translate("queryNodeEditor.filters")}>
        <TableFilters
          key={editorState.selectedInputTableIdx}
          filters={selectedTable.filters}
          onSetFilterValue={(filterIdx, value, formattedValue) =>
            onSetFilterValue(
              editorState.selectedInputTableIdx,
              filterIdx,
              value,
              formattedValue
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
              editorState.selectedInputTableIdx,
              selectedTable.id,
              node.tree,
              filterIdx,
              filterId,
              prefix
            )
          }
          suggestions={
            props.suggestions &&
            props.suggestions[editorState.selectedInputTableIdx]
          }
          onShowDescription={editorState.onShowDescription}
          onDropFilterValuesFile={(filterIdx, filterId, file) =>
            onDropFilterValuesFile(
              props.datasetId,
              node.tree,
              editorState.selectedInputTableIdx,
              selectedTable.id,
              filterIdx,
              filterId,
              file
            )
          }
          currencyConfig={props.currencyConfig}
        />
      </MaximizedCell>
    </Column>
  );
};

export default TableView;
