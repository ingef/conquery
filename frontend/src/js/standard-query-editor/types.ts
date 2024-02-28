import type {
  BigMultiSelectFilterT,
  ConceptIdT,
  DateColumnT,
  DateRangeT,
  InfoT,
  MultiSelectFilterT,
  MultiSelectFilterValueT,
  QueryIdT,
  QueryT,
  RangeFilterT,
  SelectFilterT,
  SelectFilterValueT,
  SelectorT,
  TableT,
} from "../api/types";
import { DNDType } from "../common/constants/dndTypes";

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

export interface DragContext {
  width: number;
  height: number;
  movedFromAndIdx?: number;
  movedFromOrIdx?: number;
  deleteFromOtherField?: () => void;
  movedFromFieldName?: string;
  rowPrefixFieldname?: string;
}

export interface DragItemQuery extends PreviousQueryQueryNodeType {
  dragContext: DragContext;
  type: DNDType.PREVIOUS_QUERY | DNDType.PREVIOUS_SECONDARY_ID_QUERY;
}

export interface DragItemConceptTreeNode extends ConceptQueryNodeType {
  dragContext: DragContext;
  type: DNDType.CONCEPT_TREE_NODE;
}

export interface ConceptQueryNodeType {
  ids: ConceptIdT[];
  tables: TableWithFilterValueT[];
  selects: SelectedSelectorT[];
  tree: ConceptIdT;
  description?: string;
  additionalInfos?: InfoT[];
  matchingEntries: number | null;
  matchingEntities: number | null;
  dateRange?: DateRangeT;

  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryId?: boolean;
  loading?: boolean;
  error?: string;
}

export interface PreviousQueryQueryNodeType {
  id: QueryIdT;
  query?: QueryT;
  canExpand?: boolean;
  secondaryId?: string | null;
  availableSecondaryIds?: string[];
  files?: void;

  own?: boolean;
  shared?: boolean;

  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryId?: boolean;
  loading?: boolean;
  error?: string;
  tags: string[];
}

export type StandardQueryNodeT = DragItemConceptTreeNode | DragItemQuery;

export interface QueryGroupType {
  elements: StandardQueryNodeT[];
  dateRange?: DateRangeT;
  exclude?: boolean;
}
