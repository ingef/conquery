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

interface PropsT {
  datasetId: DatasetIdT;
}

const findNodeBeingEdited = (query: StandardQueryStateT) =>
  query
    .reduce<StandardQueryNodeT[]>(
      (acc, group) => [...acc, ...group.elements],
      []
    )
    .find((element) => element.isEditing);

const StandardQueryNodeEditor = (props: PropsT) => {
  const node = useSelector<StateT, StandardQueryNodeT | undefined>((state) =>
    findNodeBeingEdited(state.queryEditor.query)
  );
  const showTables =
    node &&
    !!node.tables &&
    node.tables.some((table) => tableIsEditable(table));
  const editorState = useSelector<StateT, QueryNodeEditorStateT>(
    (state) => state.queryNodeEditor
  );
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency
  );

  const onLoadFilterSuggestions = useLoadFilterSuggestions();
  const dispatch = useDispatch();

  return (
    <QueryNodeEditor
      name="standard"
      datasetId={props.datasetId}
      node={node}
      editorState={editorState}
      showTables={showTables}
      isExcludeTimestampsPossible={true}
      isExcludeFromSecondaryIdQueryPossible={true}
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
      onSelectSelects={(value) => dispatch(setSelects(value))}
      onSelectTableSelects={(tableIdx: number, value) =>
        dispatch(setTableSelects(tableIdx, value))
      }
      onSetFilterValue={(tableIdx: number, filterIdx: number, value) =>
        dispatch(setFilterValue(tableIdx, filterIdx, value))
      }
      onSwitchFilterMode={(tableIdx: number, filterIdx: number, mode) =>
        dispatch(switchFilterMode(tableIdx, filterIdx, mode))
      }
      onResetAllFilters={(andIdx: number, orIdx: number) =>
        dispatch(resetAllFilters(andIdx, orIdx))
      }
      onToggleTimestamps={() => dispatch(toggleTimestamps())}
      onToggleSecondaryIdExclude={() => dispatch(toggleSecondaryIdExclude())}
      onSetDateColumn={(tableIdx: number, value) =>
        dispatch(setDateColumn(tableIdx, value))
      }
    />
  );
};

export default StandardQueryNodeEditor;
