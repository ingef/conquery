import { useCallback } from "react";

import {
  PostPrefixForSuggestionsParams,
  usePostPrefixForSuggestions,
} from "../../api/api";
import { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { mergeFilterOptions } from "../../model/filter";
import { NodeResetConfig } from "../../model/node";
import { resetSelects } from "../../model/select";
import {
  resetTables,
  tableIsEditable,
  tableWithDefaults,
} from "../../model/table";
import QueryNodeEditor from "../../query-node-editor/QueryNodeEditor";
import { filterSuggestionToSelectOption } from "../../query-node-editor/suggestionsHelper";
import { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import { ModeT } from "../../ui-components/InputRange";

export const EditorV2QueryNodeEditor = ({
  node,
  onClose,
  onChange,
}: {
  node: DragItemConceptTreeNode;
  onClose: () => void;
  onChange: (node: DragItemConceptTreeNode) => void;
}) => {
  const showTables =
    node.tables.length > 1 &&
    node.tables.some((table) => tableIsEditable(table));

  const onUpdateLabel = useCallback(
    (label: string) => onChange({ ...node, label }),
    [onChange, node],
  );

  const onToggleTable = useCallback(
    (tableIdx: number, isExcluded: boolean) => {
      const tables = [...node.tables];
      tables[tableIdx] = { ...tables[tableIdx], exclude: isExcluded };
      onChange({ ...node, tables });
    },
    [node, onChange],
  );

  const onDropConcept = useCallback(
    (concept: DragItemConceptTreeNode) => {
      const ids = [...node.ids, concept.ids[0]];
      onChange({ ...node, ids });
    },
    [node, onChange],
  );

  const onRemoveConcept = useCallback(
    (conceptId: string) => {
      const ids = node.ids.filter((id) => id !== conceptId);
      onChange({ ...node, ids });
    },
    [node, onChange],
  );

  const onSelectSelects = useCallback(
    (value: SelectOptionT[]) => {
      onChange({
        ...node,
        selects: node.selects.map((select) => ({
          ...select,
          selected: !!value.find((s) => s.value === select.id),
        })),
      });
    },
    [node, onChange],
  );

  const onSelectTableSelects = useCallback(
    (tableIdx: number, value: SelectOptionT[]) => {
      const tables = [...node.tables];
      tables[tableIdx] = {
        ...tables[tableIdx],
        selects: tables[tableIdx].selects.map((select) => ({
          ...select,
          selected: !!value.find((s) => s.value === select.id),
        })),
      };
      onChange({ ...node, tables });
    },
    [node, onChange],
  );

  const setFilterProperties = useCallback(
    (tableIdx: number, filterIdx: number, properties: object) => {
      const tables = [...node.tables];
      tables[tableIdx] = {
        ...tables[tableIdx],
        filters: tables[tableIdx].filters.map((filter, idx) =>
          idx === filterIdx ? { ...filter, ...properties } : filter,
        ),
      };
      onChange({ ...node, tables });
    },
    [node, onChange],
  );

  const onSetFilterValue = useCallback(
    (tableIdx: number, filterIdx: number, value: unknown) => {
      setFilterProperties(tableIdx, filterIdx, { value });
    },
    [setFilterProperties],
  );

  const onSwitchFilterMode = useCallback(
    (tableIdx: number, filterIdx: number, mode: ModeT) => {
      const tables = [...node.tables];
      tables[tableIdx] = {
        ...tables[tableIdx],
        filters: tables[tableIdx].filters.map((filter, idx) =>
          idx === filterIdx ? { ...filter, mode } : filter,
        ),
      };
      onChange({ ...node, tables });
    },
    [node, onChange],
  );

  const postPrefixForSuggestions = usePostPrefixForSuggestions();
  const onLoadFilterSuggestions = useCallback(
    async (
      params: PostPrefixForSuggestionsParams,
      tableIdx: number,
      filterIdx: number,
      config?: { returnOnly?: boolean },
    ) => {
      const suggestions = await postPrefixForSuggestions(params);

      if (!config?.returnOnly) {
        const newOptions: SelectOptionT[] = suggestions.values.map(
          filterSuggestionToSelectOption,
        );

        const filter = node.tables[tableIdx].filters[filterIdx];
        const options =
          params.page === 0
            ? newOptions
            : mergeFilterOptions(filter, newOptions);

        if (exists(options)) {
          const props = { options, total: suggestions.total };

          setFilterProperties(tableIdx, filterIdx, props);
        }
      }

      return suggestions;
    },
    [postPrefixForSuggestions, node, setFilterProperties],
  );

  const onResetTable = useCallback(
    (tableIdx: number, config: NodeResetConfig) => {
      const table = node.tables[tableIdx];
      const resetTable = tableWithDefaults(config)(table);

      const tables = [...node.tables];
      tables[tableIdx] = resetTable;
      onChange({ ...node, tables });
    },
    [node, onChange],
  );

  const onResetAllSettings = useCallback(
    (config: NodeResetConfig) => {
      const tables = resetTables(node.tables, config);
      const selects = resetSelects(node.selects, config);
      onChange({ ...node, tables, selects });
    },
    [node, onChange],
  );

  const onSetDateColumn = useCallback(
    (tableIdx: number, value: string) => {
      const tables = [...node.tables];
      tables[tableIdx] = {
        ...tables[tableIdx],
        dateColumn: {
          ...tables[tableIdx].dateColumn!,
          value,
        },
      };
      onChange({ ...node, tables });
    },
    [node, onChange],
  );

  return (
    <QueryNodeEditor
      name={`editorv2`}
      onLoadFilterSuggestions={onLoadFilterSuggestions}
      node={node}
      showTables={showTables}
      onCloseModal={onClose}
      onUpdateLabel={onUpdateLabel}
      onDropConcept={onDropConcept}
      onRemoveConcept={onRemoveConcept}
      onToggleTable={onToggleTable}
      onSelectSelects={onSelectSelects}
      onSelectTableSelects={onSelectTableSelects}
      onSetFilterValue={onSetFilterValue}
      onSwitchFilterMode={onSwitchFilterMode}
      onResetTable={onResetTable}
      onResetAllSettings={onResetAllSettings}
      onSetDateColumn={onSetDateColumn}
    />
  );
};
