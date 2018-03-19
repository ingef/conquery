// @flow

import type { TreeNodeIdType, QueryIdType } from "../common/types/backend";

// A concept that is part of a query node in the editor
export type ConceptType =  {
  id: string,
  label: string,
  description: string,
  matchingEntries: number
};

export type TableType = {
  id: number,
  label: string,
  exclude?: boolean,
  filters: ?FilterType[],
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
  concepts: Array<ConceptType>,
  tables: Array<TableType>,
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

type ConceptQueryNodeType = {
  ids: Array<TreeNodeIdType>,
  concepts: Array<ConceptType>,
  tables: TableType[],
  tree: TreeNodeIdType,

  label: string,
  excludeTimestamps?: boolean,
  loading?: boolean,
  error?: string,

  isEditing?: boolean,
  isPreviousQuery: void | false,
}

type PreviousQueryQueryNodeType = {
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

export type DateRangeType = ?{ min?: string, max?: string }

export type QueryGroupType = {
  elements: QueryNodeType[],
  dateRange?: DateRangeType,
  exclude?: boolean
};

type PreviousQueryType = {
  groups: QueryGroupType[]
};

export type StandardQueryType = QueryGroupType[];
