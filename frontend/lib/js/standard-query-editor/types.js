// @flow

import type {
  TreeNodeIdType,
  QueryIdType,
  DateRangeType,
  RangeFilterType,
  RangeFilterValueType,
  MultiSelectFilterType,
  MultiSelectFilterValueType,
  SelectFilterType,
  SelectFilterValueType
} from '../common/types/backend';

// A concept that is part of a query node in the editor
export type ConceptType =  {
  id: string,
  label: string,
  description?: string,
  matchingEntries?: number
};


export type SelectOptionType = {
  label: string,
  value: number | string,
};

export type SelectOptionsType = SelectOptionType[];

export type InfoType = {
  key: string,
  value: string,
}

export type RangeFilterWithValueType = RangeFilterType & {
  value?: RangeFilterValueType,
}

export type MultiSelectFilterWithValueType = MultiSelectFilterType & {
  value?: MultiSelectFilterValueType,
}

export type SelectFilterWithValueType = SelectFilterType & {
  value?: SelectFilterValueType,
}

export type FilterWithValueType =
  SelectFilterWithValueType
  | MultiSelectFilterWithValueType
  | RangeFilterWithValueType;

export type TableWithFilterValueType = {
  id: number,
  label: string,
  exclude?: boolean,
  filters: ?FilterWithValueType[],
};

export type DraggedFileType = {
  files: File[],
  isPreviousQuery?: void,
}

export type DraggedQueryType = {
  id: QueryIdType,
  // eslint-disable-next-line no-use-before-define
  query?: PreviousQueryType,
  label: string,
  excludeTimestamps?: boolean,

  moved?: boolean,
  andIdx?: number, orIdx?: number,  // These two only exist if moved === true

  loading?: boolean,
  error?: string,

  files?: void,
  isPreviousQuery: true
};

// A Query Node that is being dragged from the tree or within the standard editor.
// Corresponds to CATEGORY_TREE_NODE and QUERY_NODE drag-and-drop types.
export type DraggedNodeType = {
  ids: Array<TreeNodeIdType>,
  tables: Array<TableWithFilterValueType>,
  tree: TreeNodeIdType,
  label: string,
  excludeTimestamps?: boolean,

  moved?: boolean,
  andIdx?: number, orIdx?: number,  // These two only exist if moved === true

  loading?: boolean,
  error?: string,

  files?: void,
  isPreviousQuery?: void,
};

export type ConceptQueryNodeType = {
  ids: Array<TreeNodeIdType>,
  tables: TableWithFilterValueType[],
  tree: TreeNodeIdType,

  label: string,
  excludeTimestamps?: boolean,
  loading?: boolean,
  error?: string,

  isEditing?: boolean,
  isPreviousQuery?: void | false,
}

export type PreviousQueryQueryNodeType = {
  label: string,
  excludeTimestamps?: boolean,
  loading?: boolean,
  error?: string,

  id: QueryIdType,
  // eslint-disable-next-line no-use-before-define
  query?: PreviousQueryType,
  isPreviousQuery: true,
}

export type QueryNodeType = ConceptQueryNodeType | PreviousQueryQueryNodeType;

export type QueryGroupType = {
  elements: QueryNodeType[],
  dateRange?: DateRangeType,
  exclude?: boolean
};

type PreviousQueryType = {
  groups: QueryGroupType[]
};

export type StandardQueryType = QueryGroupType[];
