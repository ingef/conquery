// @flow

import React                 from 'react';

import ParameterTableFilters from './ParameterTableFilters';
import type { PropsType }    from './QueryNodeEditor';

export const TableFilterView = (props: PropsType) => {
  const { node, editorState } = props;

  const selectedTable = node.tables[editorState.selectedInputTableIdx];

  return (
    <div className="query-node-editor__large_column query-node-editor__column">
      <h4>Filter</h4>
      <div className="query-node-editor__column_content">
        <ParameterTableFilters
          key={editorState.selectedInputTableIdx}
          filters={selectedTable.filters}
          onSetFilterValue={(filterIdx, value, formattedValue) => props.onSetFilterValue(
            editorState.selectedInputTableIdx,
            filterIdx,
            value,
            formattedValue
          )}
          onSwitchFilterMode={(filterIdx, mode) => props.onSwitchFilterMode(
            editorState.selectedInputTableIdx,
            filterIdx,
            mode
          )}
          onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
            props.onLoadFilterSuggestions(
              props.datasetId,
              editorState.selectedInputTableIdx,
              selectedTable.id,
              node.tree,
              filterIdx,
              filterId,
              prefix
            )}
          suggestions={props.suggestions && props.suggestions[editorState.selectedInputTableIdx]}
          onShowDescription={editorState.onShowDescription}
          onDropFiles={(filterIdx, filterId, files) =>
            props.onDropFiles(
              props.datasetId,
              node.tree,
              editorState.selectedInputTableIdx,
              selectedTable.id,
              filterIdx,
              filterId,
              files
            )}
          currencyConfig={props.currencyConfig}
        />
      </div>
    </div>
  );
}
