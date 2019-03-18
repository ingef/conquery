// @flow

// This file specifies data types that are provided by the backend api
//
// Part of the data is saved in the redux state
// Other parts (concept trees) are stored in window.categoryTrees (see globalTreeStoreHelper)

export type SelectOptionType = {
  label: string,
  value: number | string
};

export type SelectOptionsType = SelectOptionType[];

export type DateRangeType = ?{ min?: string, max?: string };

export type InfoType = {
  key: string,
  value: string
};

export type RangeFilterValueType = {
  min?: number,
  max?: number,
  exact?: number
};
export type RangeFilterFormattedValueType = {
  min?: number,
  max?: number,
  exact?: number
};
export type RangeFilterType = {
  id: string,
  label: string,
  description?: string,
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE",
  value: ?RangeFilterValueType,
  formattedValue: ?RangeFilterFormattedValueType,
  unit?: string,
  mode: "range" | "exact",
  precision?: number,
  min?: number,
  max?: number,
  pattern?: string
};

export type MultiSelectFilterValueType = (string | number)[];
export type MultiSelectFilterType = {
  id: string,
  label: string,
  description?: string,
  type: "MULTI_SELECT",
  unit?: string,
  options: SelectOptionsType,
  defaultValue: ?MultiSelectFilterValueType
};

export type SelectFilterValueType = string | number;
export type SelectFilterType = {
  id: string,
  label: string,
  description?: string,
  type: "SELECT",
  unit?: string,
  options: SelectOptionsType,
  defaultValue: ?SelectFilterValueType
};

export type FilterType =
  | SelectFilterType
  | MultiSelectFilterType
  | RangeFilterType;

export type TableType = {
  id: string,
  label: string,
  exclude?: boolean,
  filters: ?(FilterType[])
};

export type SelectorType = {
  id: string,
  label: string,
  description: string,
  default?: boolean
};

export type TreeNodeIdType = string;
export type NodeType = {
  parent: TreeNodeIdType,
  label: string,
  description: string,
  active?: boolean,
  children: TreeNodeIdType[],
  additionalInfos?: InfoType[],
  matchingEntries?: number,
  dateRange?: DateRangeType,
  tables: TableType[],
  selects?: SelectorType[],
  detailsAvailable?: boolean,
  codeListResolvable?: boolean
};

export type RootType = {
  concepts: Map<TreeNodeIdType, NodeType>,
  version: number
};

export type ConceptListResolutionResultType = {
  resolvedConcepts?: string[],
  unknownConcepts?: string[]
};

export type FilterValuesResolutionResultType = {
  unknownCodes?: string[],
  resolvedFilter?: {
    filterId: string,
    tableId: string,
    value: {
      label: string,
      value: string
    }[]
  }
};

export type SearchResult = {
  result: string[],
  limit: number,
  size: number
};

export type QueryIdType = string;
