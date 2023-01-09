import type {
  SelectOptionT,
  PostFilterSuggestionsResponseT,
} from "../../api/types";
import { DNDType } from "../../common/constants/dndTypes";
import { compose } from "../../common/helpers/commonHelper";
import { exists } from "../../common/helpers/exists";
import {
  getConceptById,
  getConceptsByIdsWithTablesAndSelects,
} from "../../concept-trees/globalTreeStoreHelper";
import { TreesT } from "../../concept-trees/reducer";
import { mergeFilterOptions } from "../../model/filter";
import { NodeResetConfig } from "../../model/node";
import { resetSelects } from "../../model/select";
import { resetTables, tableWithDefaults } from "../../model/table";
import { filterSuggestionToSelectOption } from "../../query-node-editor/suggestionsHelper";
import type {
  DragItemConceptTreeNode,
  TableWithFilterValueT,
  FilterWithValueType,
  SelectedSelectorT,
} from "../../standard-query-editor/types";
import type { ModeT } from "../../ui-components/InputRange";
import type { ConceptListDefaults as ConceptListDefaultsType } from "../config-types";
import {
  initSelectsWithDefaults,
  initTables,
  initTablesWithDefaults,
} from "../transformers";

export type FormConceptNodeT = DragItemConceptTreeNode & {
  includeSubnodes?: boolean;
};
export interface FormConceptGroupT {
  concepts: (FormConceptNodeT | null)[];
  connector: string;
}

export interface TableConfig {
  allowlistedTables?: string[];
  blocklistedTables?: string[];
}

export const addValue = (
  value: FormConceptGroupT[],
  newValue: FormConceptGroupT,
) => [...value, newValue];

export const removeValue = (value: FormConceptGroupT[], valueIdx: number) => {
  return [...value.slice(0, valueIdx), ...value.slice(valueIdx + 1)];
};

export const setValueProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  props: Partial<FormConceptGroupT>,
) => {
  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      ...props,
    },
    ...value.slice(valueIdx + 1),
  ];
};

export const addConcept = (
  value: FormConceptGroupT[],
  valueIdx: number,
  item: FormConceptNodeT | null,
) =>
  setValueProperties(value, valueIdx, {
    concepts: [...value[valueIdx].concepts, item],
  });

export const removeConcept = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
) =>
  setValueProperties(value, valueIdx, {
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  });

export const setConcept = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  item: FormConceptNodeT,
) =>
  setValueProperties(value, valueIdx, {
    concepts: [
      ...value[valueIdx].concepts.slice(0, conceptIdx),
      item,
      ...value[valueIdx].concepts.slice(conceptIdx + 1),
    ],
  });

export const setConceptProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  props: Partial<FormConceptNodeT>,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  return concept
    ? setConcept(value, valueIdx, conceptIdx, {
        ...concept,
        ...props,
      })
    : value;
};

export const setTableProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  props: Partial<TableWithFilterValueT>,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const tables = concept.tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: [
      ...tables.slice(0, tableIdx),
      {
        ...tables[tableIdx],
        ...props,
      },
      ...tables.slice(tableIdx + 1),
    ],
  });
};

export const setFilterProperties = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  props: Partial<FilterWithValueType>,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const filters = concept.tables[tableIdx].filters;

  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    filters: [
      ...filters.slice(0, filterIdx),
      {
        ...filters[filterIdx],
        ...props,
      },
      ...filters.slice(filterIdx + 1),
    ] as FilterWithValueType[],
  });
};

export const onToggleIncludeSubnodes = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  includeSubnodes: boolean,
  newValue: FormConceptGroupT,
) => {
  const element = value[valueIdx];
  const concept = element.concepts[conceptIdx];

  if (!concept) return value;

  const conceptData = getConceptById(concept.ids[0]);

  if (!conceptData || !conceptData.children) return value;

  const childIds: string[] = [];
  const elements: FormConceptGroupT[] = conceptData.children
    .map((childId) => {
      const child = getConceptById(childId);

      if (!child) return null;

      childIds.push(childId);

      return {
        ...newValue,
        ...element,
        concepts: [
          {
            type: DNDType.CONCEPT_TREE_NODE as const,
            dragContext: {
              width: 0,
              height: 0,
            },
            matchingEntries: child.matchingEntries,
            matchingEntities: child.matchingEntities,
            ids: [childId],
            label: child.label,
            description: child.description,
            tables: concept.tables,
            selects: concept.selects,
            tree: concept.tree,
          },
        ],
      };
    })
    .filter(exists);

  const nextValue = includeSubnodes
    ? [
        ...value.slice(0, valueIdx + 1),
        // Insert right after the element
        ...elements,
        ...value.slice(valueIdx + 1),
      ]
    : value.filter((val) =>
        val.concepts.filter(exists).some((cpt) => {
          return childIds.every((childId) => !cpt.ids.includes(childId));
        }),
      );

  return setConceptProperties(
    nextValue,
    nextValue.indexOf(element),
    conceptIdx,
    {
      includeSubnodes,
    },
  );
};

export const createQueryNodeFromConceptListUploadResult = (
  label: string,
  rootConcepts: TreesT,
  resolvedConcepts: string[],
): FormConceptNodeT | null => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    rootConcepts,
    resolvedConcepts,
    { useDefaults: true },
  );

  return lookupResult
    ? {
        type: DNDType.CONCEPT_TREE_NODE as const,
        dragContext: { width: 0, height: 0 },
        matchingEntities: lookupResult.concepts[0].matchingEntities,
        matchingEntries: lookupResult.concepts[0].matchingEntries,
        label,
        ids: resolvedConcepts,
        tables: lookupResult.tables as TableWithFilterValueT[], // TODO: Convert this better
        selects: lookupResult.selects as SelectedSelectorT[], // TODO: Convert this better
        tree: lookupResult.root,
      }
    : null;
};

export const addConceptsFromFile = (
  label: string,
  rootConcepts: TreesT,
  resolvedConcepts: string[],

  tableConfig: TableConfig,
  defaults: ConceptListDefaultsType,
  isValidConcept: ((item: FormConceptNodeT) => boolean) | undefined,

  value: FormConceptGroupT[],
  newValue: FormConceptGroupT,

  valueIdx: number,
  conceptIdx?: number,

  resolvedFilter?: {
    tableId: string;
    filterId: string;
    value: SelectOptionT[];
  },
) => {
  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts,
  );

  if (!queryElement) return value;

  const concept = initializeConcept(queryElement, defaults, tableConfig);

  if (!concept || (!!isValidConcept && !isValidConcept(concept))) return value;

  if (resolvedFilter) {
    const table = concept.tables.find((t) => t.id === resolvedFilter.tableId);
    const filter = table?.filters.find((f) => f.id === resolvedFilter.filterId);

    if (table && filter) {
      filter.value = resolvedFilter.value;
    }
  }

  if (exists(conceptIdx)) {
    return setConcept(value, valueIdx, conceptIdx, concept);
  } else {
    return addConcept(addValue(value, newValue), valueIdx, concept);
  }
};

export const initializeConcept = (
  item: FormConceptNodeT,
  defaults: ConceptListDefaultsType,
  tableConfig: TableConfig,
) => {
  if (!item) return item;

  return compose(
    initSelectsWithDefaults(defaults.selects),
    initTablesWithDefaults(defaults.connectors),
    initTables(tableConfig),
  )({
    ...item,
    excludeFromSecondaryId: false,
    excludeTimestamps: false,
    tables: resetTables(item.tables, { useDefaults: true }),
    selects: resetSelects(item.selects, { useDefaults: true }),
  });
};

export const toggleTable = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  isExcluded: boolean,
) => {
  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    exclude: isExcluded,
  });
};

export const resetTable = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  config: NodeResetConfig,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const table = concept.tables[tableIdx];

  return setTableProperties(
    value,
    valueIdx,
    conceptIdx,
    tableIdx,
    tableWithDefaults(config)(table),
  );
};

export const setDateColumn = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  dateColumnValue: string,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  return concept
    ? setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
        dateColumn: {
          ...concept.tables[tableIdx].dateColumn!, // will be defined for this table, when the setter is being called
          value: dateColumnValue,
        },
      })
    : value;
};

export const setFilterValue = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  filterValue: any,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    value: filterValue,
  });
};

export const setSelects = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  selectedSelects: SelectOptionT[],
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const selects = concept.selects;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    // value contains the selects that have now been selected
    selects: selects.map((select) => ({
      ...select,
      selected: !selectedSelects
        ? false
        : !!selectedSelects.find(
            (selectedValue) => selectedValue.value === select.id,
          ),
    })),
  });
};

export const setTableSelects = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  selectedSelects: SelectOptionT[],
) => {
  const concept = value[valueIdx].concepts[conceptIdx];
  if (!concept) return value;

  const { tables } = concept;
  const selects = tables[tableIdx].selects;

  if (!selects) return value;

  return setTableProperties(value, valueIdx, conceptIdx, tableIdx, {
    // value contains the selects that have now been selected
    selects: selects.map((select) => ({
      ...select,
      selected: !selectedSelects
        ? false
        : !!selectedSelects.find(
            (selectedValue) => selectedValue.value === select.id,
          ),
    })),
  });
};

export const resetAllSettings = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  config: NodeResetConfig,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];
  if (!concept) return value;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    excludeFromSecondaryId: false,
    excludeTimestamps: false,
    selects: resetSelects(concept.selects, config),
    tables: resetTables(concept.tables, config),
  });
};

export const switchFilterMode = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  mode: ModeT,
) => {
  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    mode: mode,
    value: null,
  });
};

export const copyConcept = (item: FormConceptNodeT | null) => {
  return JSON.parse(JSON.stringify(item));
};

export const updateFilterOptionsWithSuggestions = (
  value: FormConceptGroupT[],
  valueIdx: number,
  conceptIdx: number,
  tableIdx: number,
  filterIdx: number,
  data: PostFilterSuggestionsResponseT,
  page: number,
) => {
  const concept = value[valueIdx].concepts[conceptIdx];

  if (!concept) return value;

  const filter = concept.tables[tableIdx].filters[filterIdx];

  const newOptions: SelectOptionT[] = data.values.map(
    filterSuggestionToSelectOption,
  );

  const options =
    page === 0 ? newOptions : mergeFilterOptions(filter, newOptions);

  if (!exists(options)) return value;

  return setFilterProperties(value, valueIdx, conceptIdx, tableIdx, filterIdx, {
    options,
    total: data.total,
  });
};
