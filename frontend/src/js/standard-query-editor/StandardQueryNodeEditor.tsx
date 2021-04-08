import React from "react";
import { StateT } from "app-types";
import { useDispatch, useSelector } from "react-redux";

import QueryNodeEditor from "../query-node-editor/QueryNodeEditor";

import { tableIsEditable } from "../model/table";
import { ConceptIdT, CurrencyConfigT, DatasetIdT } from "../api/types";
import { QueryNodeEditorStateT } from "../query-node-editor/reducer";

import {
  deselectNode,
  updateNodeLabel,
  addConceptToNode,
  removeConceptFromNode,
  toggleTable,
  setFilterValue,
  switchFilterMode,
  resetAllFilters,
  toggleTimestamps,
  setSelects,
  setTableSelects,
  setDateColumn,
  toggleSecondaryIdExclude,
  useLoadFilterSuggestions,
} from "./actions";
import { StandardQueryNodeT } from "./types";
import type { StandardQueryStateT } from "./queryReducer";
import { isConceptQueryNode } from "../model/query";

const findNodeBeingEdited = (query: StandardQueryStateT) =>
  query
    .reduce<StandardQueryNodeT[]>(
      (acc, group) => [...acc, ...group.elements],
      []
    )
    .find((element) => element.isEditing);

const StandardQueryNodeEditor = () => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const node = useSelector<StateT, StandardQueryNodeT | undefined>((state) =>
    findNodeBeingEdited(state.queryEditor.query)
  );
  const showTables =
    !!node &&
    isConceptQueryNode(node) &&
    !!node.tables &&
    node.tables.length > 1 &&
    node.tables.some((table) => tableIsEditable(table));
  const editorState = useSelector<StateT, QueryNodeEditorStateT>(
    (state) => state.queryNodeEditor
  );
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency
  );

  const onLoadFilterSuggestions = useLoadFilterSuggestions();
  const dispatch = useDispatch();

  if (!datasetId || !node) {
    return null;
  }

  return (
    <QueryNodeEditor
      name="standard"
      datasetId={datasetId}
      node={node}
      editorState={editorState}
      showTables={showTables}
      currencyConfig={currencyConfig}
      onLoadFilterSuggestions={onLoadFilterSuggestions}
      onCloseModal={() => dispatch(deselectNode())}
      onUpdateLabel={(label: string) => dispatch(updateNodeLabel(label))}
      onDropConcept={(concept) => dispatch(addConceptToNode(concept))}
      onRemoveConcept={(conceptId: ConceptIdT) =>
        dispatch(removeConceptFromNode(conceptId))
      }
      onToggleTable={(tableIdx: number, isExcluded: boolean) =>
        dispatch(toggleTable(tableIdx, isExcluded))
      }
      onSelectSelects={(value) => {
        dispatch(setSelects(value));
      }}
      onSelectTableSelects={(tableIdx: number, value) =>
        dispatch(setTableSelects(tableIdx, value))
      }
      onSetFilterValue={(tableIdx: number, filterIdx: number, value) =>
        dispatch(setFilterValue(tableIdx, filterIdx, value))
      }
      onSwitchFilterMode={(...args) => dispatch(switchFilterMode(...args))}
      onResetAllFilters={() => dispatch(resetAllFilters())}
      onToggleTimestamps={() => dispatch(toggleTimestamps())}
      onToggleSecondaryIdExclude={() => dispatch(toggleSecondaryIdExclude())}
      onSetDateColumn={(tableIdx: number, value) =>
        dispatch(setDateColumn(tableIdx, value))
      }
    />
  );
};

export default StandardQueryNodeEditor;
