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
          onSetFilterValue={(filterIdx, value) => props.onSetFilterValue(
            editorState.selectedInputTableIdx,
            filterIdx,
            value
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
              node.id,
              filterIdx,
              filterId,
              prefix
            )}
          suggestions={props.suggestions && props.suggestions[editorState.selectedInputTableIdx]}
          onShowDescription={editorState.onShowDescription}
        />
      </div>
    </div>
  );
}
