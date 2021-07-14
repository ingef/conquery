// This file specifies
// - response type provided by the backend API
// - partial types that the reponses are built from
import { Forms } from "../external-forms/config-types";
import type { FormConfigT } from "../external-forms/form-configs/reducer";

export type DatasetIdT = string;
export interface DatasetT {
  id: DatasetIdT;
  label: string;
}

export interface SelectOptionT {
  label: string;
  value: number | string;
}

// Example: { min: "2019-01-01", max: "2019-12-31" }
export interface DateRangeT {
  min?: string;
  max?: string;
}

export interface CurrencyConfigT {
  prefix: string;
  thousandSeparator: string;
  decimalSeparator: string;
  decimalScale: number;
}

export type FilterIdT = string;
export interface FilterBaseT {
  id: FilterIdT;
  label: string;
  description?: string;
}

export interface RangeFilterValueT {
  min?: number;
  max?: number;
  exact?: number;
}
export interface RangeFilterT extends FilterBaseT {
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE";
  value: RangeFilterValueT | null;
  defaultValue?: RangeFilterValueT;
  unit?: string;
  mode: "range" | "exact";
  precision?: number;
  min?: number;
  max?: number;
  pattern?: string;
}

export type MultiSelectFilterValueT = SelectOptionT[] | FilterSuggestion[];
export interface MultiSelectFilterBaseT extends FilterBaseT {
  unit?: string;
  options: SelectOptionT[] | FilterSuggestion[];
  defaultValue?: string[];
}

export interface MultiSelectFilterT extends MultiSelectFilterBaseT {
  type: "MULTI_SELECT";
}

export interface BigMultiSelectFilterT extends MultiSelectFilterBaseT {
  type: "BIG_MULTI_SELECT";
  allowDropFile: boolean;
  // Not needed in this format:
  template: {
    filePath: string; // "/.../import/stable/Referenzen/example.csv",
    columns: string[];
    columnValue: string; // Unclear, what that's even needed for
    value: string;
    optionValue: string;
  };
}

export type SelectFilterValueT = string | number;
export interface SelectFilterT extends FilterBaseT {
  type: "SELECT";
  unit?: string;
  options: SelectOptionT[];
  defaultValue: SelectFilterValueT | null;
}

export type StringFilterValueT = string;
export interface StringFilterT extends FilterBaseT {
  type: "STRING";
}

export interface DateColumnT {
  options: SelectOptionT[];
  defaultValue: string | null;
  value?: string;
}

export type FilterT =
  | StringFilterT
  | SelectFilterT
  | MultiSelectFilterT
  | RangeFilterT
  | BigMultiSelectFilterT;

export type TableIdT = string;
export interface TableT {
  id: TableIdT;
  dateColumn: DateColumnT | null;
  connectorId: string; // TODO: Get rid of two ids here (unclear when which one should be used)
  label: string;
  exclude?: boolean;
  filters?: FilterT[]; // Empty array: key not defined
  selects?: SelectorT[]; // Empty array: key not defined
  supportedSecondaryIds?: string[];
}

export type SelectorIdT = string;
export interface SelectorT {
  id: SelectorIdT;
  label: string;
  description: string;
  default?: boolean;
}

export interface InfoT {
  key: string;
  value: string;
}

export type ConceptIdT = string;

export interface ConceptBaseT {
  label: string;
  active: boolean;
  detailsAvailable: boolean;
  codeListResolvable: boolean;
  matchingEntries: number; // TODO: Don't send with struct nodes (even sent with 0)
  children?: ConceptIdT[]; // Might be an empty struct or a "virtual node"
  description?: string; // Empty array: key not defined
  additionalInfos?: InfoT[]; // Empty array: key not defined
  dateRange?: DateRangeT;
}

export type ConceptStructT = ConceptBaseT;

export interface ConceptElementT extends ConceptBaseT {
  parent?: ConceptIdT; // If not set, it's nested under a struct node
  tables?: TableT[]; // Empty array: key not defined
  selects?: SelectorT[]; // Empty array: key not defined
}

export type ConceptT = ConceptElementT | ConceptStructT;

export interface FilterConfigT {
  filter: FilterIdT; // TODO: Rename this: "id"
  type:
    | "INTEGER_RANGE"
    | "REAL_RANGE"
    | "MONEY_RANGE"
    | "STRING"
    | "SELECT"
    | "MULTI_SELECT"
    | "BIG_MULTI_SELECT";
  value:
    | StringFilterValueT
    | RangeFilterValueT
    | SelectFilterValueT
    | FilterIdT[]; // Multi select
}

export interface DateColumnConfigT {
  value: string;
}

export interface TableConfigT {
  id: TableIdT;
  filters?: FilterConfigT[];
  dateColumn?: DateColumnConfigT;
  selects: SelectorIdT[];
}

export interface QueryConceptNodeT {
  type: "CONCEPT";
  ids: ConceptIdT[];
  label?: string; // Used to expand
  excludeFromTimeAggregation: boolean; // TODO: Not used
  excludeFromSecondaryIdQuery: boolean;
  tables: TableConfigT[];
  selects?: SelectorIdT[];
}

export type QueryIdT = string;
export interface SavedQueryNodeT {
  type: "SAVED_QUERY";
  query: QueryIdT; // TODO: rename this "id"
}

export interface OrNodeT {
  type: "OR";
  children: (QueryConceptNodeT | SavedQueryNodeT)[];
}

export interface DateRestrictionNodeT {
  type: "DATE_RESTRICTION";
  dateRange: DateRangeT;
  child: OrNodeT;
}

export interface NegationNodeT {
  type: "NEGATION";
  child: DateRestrictionNodeT | OrNodeT;
}

export interface AndNodeT {
  type: "AND";
  children: (DateRestrictionNodeT | NegationNodeT | OrNodeT)[];
}
interface BaseQueryT {
  type: "CONCEPT_QUERY";
}

export interface AndQueryT extends BaseQueryT {
  secondaryId?: string;
  root: AndNodeT;
}
export interface NegationQueryT extends BaseQueryT {
  root: NegationNodeT;
}
export interface DateRestrictionQueryT extends BaseQueryT {
  root: DateRestrictionNodeT;
}
export type QueryT = AndQueryT | NegationQueryT | DateRestrictionQueryT;
export type QueryNodeT =
  | AndNodeT
  | NegationNodeT
  | DateRestrictionNodeT
  | OrNodeT
  | QueryConceptNodeT
  | SavedQueryNodeT;

// ---------------------------------------
// ---------------------------------------
// API RESPONSES
// ---------------------------------------
// ---------------------------------------
export type GetDatasetsResponseT = DatasetT[];

export interface GetFrontendConfigResponseT {
  currency: CurrencyConfigT;
  version: string;
}

export type GetConceptResponseT = Record<ConceptIdT, ConceptElementT>;

export interface SecondaryId {
  id: string;
  label: string;
  description?: string;
}

export interface GetConceptsResponseT {
  secondaryIds: SecondaryId[];
  concepts: {
    [conceptId: string]: ConceptStructT | ConceptElementT;
  };
  version?: number; // TODO: Is this even sent anymore?
}

// TODO: This actually returns GETQueryResponseT => a lot of unused fields
export interface PostQueriesResponseT {
  id: QueryIdT;
}

export type ColumnDescriptionKind =
  | "BOOLEAN"
  | "STRING"
  | "INTEGER"
  | "MONEY"
  | "NUMERIC"
  | "DATE"
  | "DATE_RANGE"
  | "LIST[DATE_RANGE]"
  | "CATEGORICAL"
  | "RESOLUTION";

export interface ColumnDescription {
  label: string;
  selectId: string | null;
  type: ColumnDescriptionKind;
}

// TODO: This actually returns GETQueryResponseT => a lot of unused fields
export interface GetQueryResponseDoneT {
  status: "DONE" | "NEW";
  numberOfResults: number | null;
  resultUrls: string[];
  columnDescriptions: ColumnDescription[] | null;
  queryType: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
  requiredTime: number; // In ms, unused at the moment
}

export interface GetQueryRunningResponseT {
  status: "RUNNING";
  progress: number | null;
}

// TODO: This actually returns GETQueryResponseT => a lot of unused fields
export interface GetQueryErrorResponseT {
  status: "FAILED" | "CANCELED";
  error: ErrorResponseT | null;
}

export interface ErrorResponseT {
  code: string; // To translate to localized messages
  message?: string; // For developers / debugging only
  context?: Record<string, string>; // More information to maybe display in translated messages
}

export type GetQueryResponseStatusT =
  | GetQueryRunningResponseT
  | GetQueryResponseDoneT
  | GetQueryErrorResponseT;

interface GetQueryResponseCommon {
  id: QueryIdT;
  label: string;
  createdAt: string; // ISO timestamp: 2019-06-18T11:11:50.528626+02:00
  own: boolean;
  shared: boolean;
  system: boolean;
  tags: string[];
  query: QueryT;
  secondaryId: string | null;
  owner: string; // TODO: Remove. Not used. And it's actually an ID
  ownerName: string;
  groups?: UserGroupIdT[];
  canExpand?: boolean;
  availableSecondaryIds?: string[];
}

export type GetQueryResponseT = GetQueryResponseCommon &
  GetQueryResponseStatusT;

// TODO: This actually returns a lot of unused fields, see above
// TODO: But actually, it's not correct, because some fields are not
//       returned on the LIST response, which ARE returned in the
//       single result response
export type GetQueriesResponseT = (GetQueryResponseCommon &
  GetQueryResponseDoneT)[];

export interface PostConceptResolveResponseT {
  resolvedConcepts?: ConceptIdT[];
  unknownCodes?: ConceptIdT[]; // TODO: Use "unknownConcepts"
}

export interface PostFilterResolveResponseT {
  unknownCodes?: string[];
  resolvedFilter?: {
    filterId: FilterIdT;
    tableId: TableIdT;
    value: {
      label: string;
      value: string;
    }[];
  };
}

export interface FilterSuggestion {
  label: string;
  value: string;
  optionValue: string;
  templateValues: Record<string, string>;
}
export type PostFilterSuggestionsResponseT = FilterSuggestion[];

export type GetFormQueriesResponseT = Forms;

export interface PermissionsT {
  [permission: string]: boolean;
}

export type UserGroupIdT = string;
export interface UserGroupT {
  id: UserGroupIdT;
  label: string;
}

export interface GetMeResponseT {
  userName: string;
  datasetAbilities: Record<DatasetIdT, PermissionsT>;
  groups: UserGroupT[];
  hideLogoutButton?: boolean;
}

export interface PostLoginResponseT {
  access_token: string;
}

export interface PostFormConfigsResponseT {
  id: string;
}

export type GetFormConfigsResponseT = FormConfigT[];

export type GetFormConfigResponseT = FormConfigT;
