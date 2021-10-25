import { StateT } from "app-types";
import React from "react";
import { useDispatch, useSelector } from "react-redux";

import { ConceptIdT, CurrencyConfigT, DatasetIdT } from "../api/types";
import { nodeIsConceptQueryNode } from "../model/node";
import { tableIsEditable } from "../model/table";
import QueryNodeEditor from "../query-node-editor/QueryNodeEditor";
import { QueryNodeEditorStateT } from "../query-node-editor/reducer";

import {
  updateNodeLabel,
  addConceptToNode,
  removeConceptFromNode,
  toggleTable,
  setFilterValue,
  switchFilterMode,
  resetAllFilters,
  resetTable,
  toggleTimestamps,
  setSelects,
  setTableSelects,
  setDateColumn,
  toggleSecondaryIdExclude,
  useLoadFilterSuggestions,
} from "./actions";
import { StandardQueryNodeT } from "./types";

interface EditedNodePosition {
  andIdx: number;
  orIdx: number;
}

interface Props {
  editedNode: EditedNodePosition | null;
  setEditedNode: (node: EditedNodePosition | null) => void;
}

const StandardQueryNodeEditor = ({ editedNode, setEditedNode }: Props) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const node = useSelector<StateT, StandardQueryNodeT | null>((state) =>
    editedNode
      ? state.queryEditor.query[editedNode.andIdx]?.elements[editedNode.orIdx]
      : null,
  );
  const showTables =
    !!node &&
    nodeIsConceptQueryNode(node) &&
    !!node.tables &&
    node.tables.length > 1 &&
    node.tables.some((table) => tableIsEditable(table));
  const editorState = useSelector<StateT, QueryNodeEditorStateT>(
    (state) => state.queryNodeEditor,
  );
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );

  const onLoadFilterSuggestions = useLoadFilterSuggestions(editedNode);
  const dispatch = useDispatch();

  if (!datasetId || !node || !editedNode) {
    return null;
  }

  const { andIdx, orIdx } = editedNode;

  return (
    <QueryNodeEditor
      name="standard"
      datasetId={datasetId}
      node={node}
      editorState={editorState}
      showTables={showTables}
      currencyConfig={currencyConfig}
      onLoadFilterSuggestions={onLoadFilterSuggestions}
      onCloseModal={() => setEditedNode(null)}
      onUpdateLabel={(label: string) =>
        dispatch(updateNodeLabel({ andIdx, orIdx, label }))
      }
      onDropConcept={(concept) =>
        dispatch(addConceptToNode({ andIdx, orIdx, concept }))
      }
      onRemoveConcept={(conceptId: ConceptIdT) =>
        dispatch(removeConceptFromNode({ andIdx, orIdx, conceptId }))
      }
      onToggleTable={(tableIdx: number, isExcluded: boolean) =>
        dispatch(toggleTable({ andIdx, orIdx, tableIdx, isExcluded }))
      }
      onSelectSelects={(value) => {
        dispatch(setSelects({ andIdx, orIdx, value }));
      }}
      onSelectTableSelects={(tableIdx: number, value) =>
        dispatch(setTableSelects({ andIdx, orIdx, tableIdx, value }))
      }
      onSetFilterValue={(tableIdx: number, filterIdx: number, value) =>
        dispatch(setFilterValue({ andIdx, orIdx, tableIdx, filterIdx, value }))
      }
      onSwitchFilterMode={(tableIdx, filterIdx, mode) =>
        dispatch(switchFilterMode({ andIdx, orIdx, tableIdx, filterIdx, mode }))
      }
      onResetAllFilters={() => dispatch(resetAllFilters({ andIdx, orIdx }))}
      onResetTable={(tableIdx: number) =>
        dispatch(resetTable({ andIdx, orIdx, tableIdx }))
      }
      onToggleTimestamps={() => dispatch(toggleTimestamps({ andIdx, orIdx }))}
      onToggleSecondaryIdExclude={() =>
        dispatch(toggleSecondaryIdExclude({ andIdx, orIdx }))
      }
      onSetDateColumn={(tableIdx: number, value) =>
        dispatch(setDateColumn({ andIdx, orIdx, tableIdx, value }))
      }
    />
  );
};

export default StandardQueryNodeEditor;
