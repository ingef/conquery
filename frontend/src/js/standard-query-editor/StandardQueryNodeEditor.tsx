import { StateT } from "app-types";
import { useDispatch, useSelector } from "react-redux";

import { ConceptIdT, CurrencyConfigT, DatasetIdT } from "../api/types";
import { nodeIsConceptQueryNode } from "../model/node";
import { tableIsEditable } from "../model/table";
import QueryNodeEditor from "../query-node-editor/QueryNodeEditor";

import {
  updateNodeLabel,
  addConceptToNode,
  removeConceptFromNode,
  toggleTable,
  setFilterValue,
  switchFilterMode,
  resetAllSettings,
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
      onToggleTable={(tableIdx, isExcluded) =>
        dispatch(toggleTable({ andIdx, orIdx, tableIdx, isExcluded }))
      }
      onSelectSelects={(value) => {
        dispatch(setSelects({ andIdx, orIdx, value }));
      }}
      onSelectTableSelects={(tableIdx, value) =>
        dispatch(setTableSelects({ andIdx, orIdx, tableIdx, value }))
      }
      onSetFilterValue={(tableIdx, filterIdx, value) =>
        dispatch(setFilterValue({ andIdx, orIdx, tableIdx, filterIdx, value }))
      }
      onSwitchFilterMode={(tableIdx, filterIdx, mode) =>
        dispatch(switchFilterMode({ andIdx, orIdx, tableIdx, filterIdx, mode }))
      }
      onResetAllSettings={(config) =>
        dispatch(resetAllSettings({ andIdx, orIdx, config }))
      }
      onResetTable={(tableIdx, config) =>
        dispatch(resetTable({ andIdx, orIdx, tableIdx, config }))
      }
      onToggleTimestamps={() => dispatch(toggleTimestamps({ andIdx, orIdx }))}
      onToggleSecondaryIdExclude={() =>
        dispatch(toggleSecondaryIdExclude({ andIdx, orIdx }))
      }
      onSetDateColumn={(tableIdx, value) =>
        dispatch(setDateColumn({ andIdx, orIdx, tableIdx, value }))
      }
    />
  );
};

export default StandardQueryNodeEditor;
