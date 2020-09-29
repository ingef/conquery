import type {
  ConceptIdT,
  QueryIdT,
  RangeFilterT,
  RangeFilterValueT,
  MultiSelectFilterT,
  MultiSelectFilterValueT,
  SelectFilterT,
  SelectFilterValueT,
  SelectorT,
  TableT,
  DateRangeT,
  DateColumnT
} from "../api/types";

// A concept that is part of a query node in the editor
export type ConceptType = {
  id: string;
  label: string;
  description?: string;
  matchingEntries?: number;
};

export type SelectOptionType = {
  label: string;
  value: number | string;
};

export type SelectOptionsType = SelectOptionType[];

export type InfoType = {
  key: string;
  value: string;
};

export type RangeFilterWithValueType = RangeFilterT & {
  value?: RangeFilterValueT;
};

export type MultiSelectFilterWithValueType = MultiSelectFilterT & {
  value?: MultiSelectFilterValueT;
};

export type SelectFilterWithValueType = SelectFilterT & {
  value?: SelectFilterValueT;
};

export type FilterWithValueType =
  | SelectFilterWithValueType
  | MultiSelectFilterWithValueType
  | RangeFilterWithValueType;

export type SelectedSelectorType = SelectorT & {
  selected?: boolean;
};

export type SelectedDateColumnT = DateColumnT & {
  value?: string;
};

export type TableWithFilterValueType = TableT & {
  filters: ?FilterWithValueType[];
  selects?: SelectedSelectorType[];
  dateColumn?: SelectedDateColumnT;
};

export type DraggedQueryType = {
  id: QueryIdT;
  // eslint-disable-next-line no-use-before-define
  query?: PreviousQueryType;
  label: string;
  excludeTimestamps?: boolean;

  moved?: boolean;
  andIdx?: number;
  orIdx?: number; // These two only exist if moved === true

  loading?: boolean;
  error?: string;

  files?: void;
  isPreviousQuery: true;
};

// A Query Node that is being dragged from the tree or within the standard editor.
// Corresponds to CONCEPT_TREE_NODE and QUERY_NODE drag-and-drop types.
export type DraggedNodeType = {
  ids: ConceptIdT[];
  tables: TableWithFilterValueType[];
  selects: SelectedSelectorType[];
  tree: ConceptIdT;
  label: string;
  excludeTimestamps?: boolean;

  additionalInfos: Object;
  matchingEntries: number;
  dateRange: Object;

  moved?: boolean;
  andIdx?: number;
  orIdx?: number; // These two only exist if moved === true

  loading?: boolean;
  error?: string;

  files?: void;
  isPreviousQuery?: void;
};

export type ConceptQueryNodeType = {
  ids: ConceptIdT[];
  tables: TableWithFilterValueType[];
  selects: SelectedSelectorType[];
  tree: ConceptIdT;

  label: string;
  description?: string;
  excludeTimestamps?: boolean;
  loading?: boolean;
  error?: string;

  isEditing?: boolean;
  isPreviousQuery?: void | false;
};

export type PreviousQueryQueryNodeType = {
  label: string;
  excludeTimestamps?: boolean;
  loading?: boolean;
  error?: string;

  id: QueryIdT;
  // eslint-disable-next-line no-use-before-define
  query?: PreviousQueryType;
  isPreviousQuery: true;
};

export type QueryNodeType = ConceptQueryNodeType | PreviousQueryQueryNodeType;

export type QueryGroupType = {
  elements: QueryNodeType[];
  dateRange?: DateRangeT;
  exclude?: boolean;
};

type PreviousQueryType = {
  groups: QueryGroupType[];
};

export type StandardQueryType = QueryGroupType[];
