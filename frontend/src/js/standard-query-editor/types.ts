import type {
  ConceptIdT,
  QueryIdT,
  RangeFilterT,
  MultiSelectFilterT,
  MultiSelectFilterValueT,
  SelectFilterT,
  SelectFilterValueT,
  SelectorT,
  TableT,
  DateRangeT,
  DateColumnT,
} from "../api/types";

// A concept that is part of a query node in the editor
export interface ConceptType {
  id: string;
  label: string;
  description?: string;
  matchingEntries?: number;
}

export interface InfoType {
  key: string;
  value: string;
}

export type RangeFilterWithValueType = RangeFilterT;

export interface MultiSelectFilterWithValueType extends MultiSelectFilterT {
  value?: MultiSelectFilterValueT;
}

export interface SelectFilterWithValueType extends SelectFilterT {
  value?: SelectFilterValueT;
}

export type FilterWithValueType =
  | SelectFilterWithValueType
  | MultiSelectFilterWithValueType
  | RangeFilterWithValueType;

export interface SelectedSelectorType extends SelectorT {
  selected?: boolean;
}

export interface SelectedDateColumnT extends DateColumnT {
  value?: string;
}

export interface TableWithFilterValueType
  extends Omit<TableT, "filters" | "selects" | "dateColumn"> {
  filters: FilterWithValueType[] | null;
  selects?: SelectedSelectorType[];
  dateColumn?: SelectedDateColumnT;
}

export interface DraggedQueryType {
  id: QueryIdT;

  // drag info;
  type: string;
  width: number;
  height: number;

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
  isPreviousQuery: boolean; // true

  canExpand?: boolean;
}

// A Query Node that is being dragged from the tree or within the standard editor.
// Corresponds to CONCEPT_TREE_NODE and QUERY_NODE drag-and-drop types.
export interface DraggedNodeType {
  ids: ConceptIdT[];
  tables: TableWithFilterValueType[];
  selects: SelectedSelectorType[];
  tree: ConceptIdT;
  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryIdQuery?: boolean;

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
}

export interface ConceptQueryNodeType {
  ids: ConceptIdT[];
  tables: TableWithFilterValueType[];
  selects: SelectedSelectorType[];
  tree: ConceptIdT;

  label: string;
  description?: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryIdQuery?: boolean;
  loading?: boolean;
  error?: string;

  isEditing?: boolean;
  isPreviousQuery?: void | false;
}

export interface PreviousQueryQueryNodeType {
  label: string;
  excludeTimestamps?: boolean;
  loading?: boolean;
  error?: string;

  id: QueryIdT;
  // eslint-disable-next-line no-use-before-define
  query?: PreviousQueryType;
  isPreviousQuery: true;
  canExpand?: boolean;
  isEditing?: boolean;
}

export type QueryNodeType = ConceptQueryNodeType | PreviousQueryQueryNodeType;

export interface QueryGroupType {
  elements: QueryNodeType[];
  dateRange?: DateRangeT;
  exclude?: boolean;
}

interface PreviousQueryType {
  groups: QueryGroupType[];
}

export type StandardQueryType = QueryGroupType[];
