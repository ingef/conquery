// @flow

import React from "react";

import ParameterTableFilters from "./ParameterTableFilters";
import ColumnWithContent from "./ColumnWithContent";
import type { PropsType } from "./QueryNodeEditor";

export const TableFilterView = (props: PropsType) => {
  const { node, editorState } = props;

  const selectedTable = node.tables[editorState.selectedInputTableIdx];

  return (
    <ColumnWithContent headline={"Filter"}>
      <ParameterTableFilters
        key={editorState.selectedInputTableIdx}
        filters={selectedTable.filters}
        onSetFilterValue={(filterIdx, value, formattedValue) =>
          props.onSetFilterValue(
            editorState.selectedInputTableIdx,
            filterIdx,
            value,
            formattedValue
          )
        }
        onSwitchFilterMode={(filterIdx, mode) =>
          props.onSwitchFilterMode(
            editorState.selectedInputTableIdx,
            filterIdx,
            mode
          )
        }
        onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
          props.onLoadFilterSuggestions(
            props.datasetId,
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
          props.onDropFilterValuesFile(
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
    </ColumnWithContent>
  );
};
