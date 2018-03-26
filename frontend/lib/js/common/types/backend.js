// @flow

// This file specifies data types that are provided by the backend api
// and subsequently stored in window.categoryTrees (see globalTreeStoreHelper)

export type SelectOptionType = {
  label: string,
  value: number | string,
};

export type SelectOptionsType = SelectOptionType[];

export type DateRangeType = ?{ min?: string, max?: string }

export type InfoType = {
  key: string,
  value: string,
}

export type RangeFilterValueType = { min?: number, max?: number, exact?: number }
export type RangeFilterFormattedValueType = { min?: number, max?: number, exact?: number }
export type RangeFilterType = {
  id: number,
  label: string,
  description?: string,
  type: 'INTEGER_RANGE' | 'REAL_RANGE' | 'MONEY_RANGE',
  value: ?RangeFilterValueType,
  formattedValue: ?RangeFilterFormattedValueType,
  unit?: string,
  mode: 'range' | 'exact',
  precision?: number,
  min?: number,
  max?: number,
}

export type MultiSelectFilterValueType = (string | number)[];
export type MultiSelectFilterType = {
  id: number,
  label: string,
  description?: string,
  type: 'MULTI_SELECT',
  unit?: string,
  options: SelectOptionsType,
  defaultValue: ?MultiSelectFilterValueType
}

export type SelectFilterValueType = string | number;
export type SelectFilterType = {
  id: number,
  label: string,
  description?: string,
  type: 'SELECT',
  unit?: string,
  options: SelectOptionsType,
  defaultValue: ?SelectFilterValueType
}

export type FilterType = SelectFilterType | MultiSelectFilterType | RangeFilterType;

export type TreeNodeIdType = string;
export type QueryIdType = string;

export type TableType = {
    id: number,
    label: string,
    exclude?: boolean,
    filters: ?FilterType[],
  };

export type NodeType = {
  parent: TreeNodeIdType,
  label: string,
  description: string,
  active?: boolean,
  children: Array<TreeNodeIdType>,
  additionalInfos?: Array<InfoType>,
  matchingEntries?: number,
  dateRange?: DateRangeType,
  tables: Array<TableType>,
  detailsAvailable?: boolean,
  codeListResolvable?: boolean,
}

export type RootType = {
  concepts: Map<TreeNodeIdType, NodeType>,
  version: number
};

export type ConceptListResolutionResultType = {
  resolvedConcepts?: string[],
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
