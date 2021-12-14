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
  QueryT,
  BigMultiSelectFilterT,
  InfoT,
} from "../api/types";

export interface InfoType {
  key: string;
  value: string;
}

export type RangeFilterWithValueType = RangeFilterT;

export interface MultiSelectFilterWithValueType extends MultiSelectFilterT {
  value?: MultiSelectFilterValueT;
}
export interface BigMultiSelectFilterWithValueType
  extends BigMultiSelectFilterT {
  value?: MultiSelectFilterValueT;
}

export interface SelectFilterWithValueType extends SelectFilterT {
  value: SelectFilterValueT | null;
}

export type FilterWithValueType =
  | SelectFilterWithValueType
  | MultiSelectFilterWithValueType
  | BigMultiSelectFilterWithValueType
  | RangeFilterWithValueType;

export interface SelectedSelectorT extends SelectorT {
  selected?: boolean;
}

export interface SelectedDateColumnT extends DateColumnT {
  value: string;
}

export interface TableWithFilterValueT
  extends Omit<TableT, "filters" | "selects" | "dateColumn"> {
  filters: FilterWithValueType[];
  selects: SelectedSelectorT[];
  dateColumn?: SelectedDateColumnT;
}

export interface DragItemQuery {
  // drag info;
  type: "PREVIOUS_QUERY" | "PREVIOUS_SECONDARY_ID_QUERY";
  width: number;
  height: number;

  id: QueryIdT;
  label: string;
  excludeTimestamps?: boolean;

  loading?: boolean;
  error?: string;

  files?: void;
  isPreviousQuery: boolean; // true

  canExpand?: boolean;

  secondaryId?: string | null;
  availableSecondaryIds?: string[];
  excludeFromSecondaryId?: boolean;
  tags: string[];

  own?: boolean;
  shared?: boolean;
}

// ------------------
// A Query Node that is being dragged around within the standard editor.
interface DragItemNodeConcept extends Omit<DragItemConceptTreeNode, "type"> {
  type: "QUERY_NODE";
  moved: true;
  andIdx: number;
  orIdx: number;
}
interface DragItemNodeQuery extends Omit<DragItemQuery, "type"> {
  type: "QUERY_NODE";
  moved: true;
  andIdx: number;
  orIdx: number;
}
export type DragItemNode = DragItemNodeConcept | DragItemNodeQuery;
// ------------------

export interface DragItemConceptTreeNode extends ConceptQueryNodeType {
  type: "CONCEPT_TREE_NODE";
  height: number;
  width: number;
}

export interface ConceptQueryNodeType {
  ids: ConceptIdT[];
  tables: TableWithFilterValueT[];
  selects: SelectedSelectorT[];
  tree: ConceptIdT;
  description?: string;
  additionalInfos?: InfoT[];
  matchingEntries?: number;
  matchingEntities?: number;
  dateRange?: DateRangeT;

  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryId?: boolean;
  loading?: boolean;
  error?: string;

  isPreviousQuery?: false;
}

export interface PreviousQueryQueryNodeType {
  id: QueryIdT;
  query?: QueryT;
  canExpand?: boolean;
  secondaryId?: string | null;
  availableSecondaryIds?: string[];

  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryId?: boolean;
  loading?: boolean;
  error?: string;

  isPreviousQuery: true;
}

export type StandardQueryNodeT =
  | ConceptQueryNodeType
  | PreviousQueryQueryNodeType;

export interface QueryGroupType {
  elements: StandardQueryNodeT[];
  dateRange?: DateRangeT;
  exclude?: boolean;
}
