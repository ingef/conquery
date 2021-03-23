// This file specifies
// - response type provided by the backend API
// - partial types that the reponses are built from

import { Forms } from "../external-forms/config-types";
import type { FormConfigT } from "../external-forms/form-configs/reducer";
import { SupportedErrorCodesT } from "./errorCodes";

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
  unit?: string;
  mode: "range" | "exact";
  precision?: number;
  min?: number;
  max?: number;
  pattern?: string;
}

export type MultiSelectFilterValueT = (string | number)[];
export interface MultiSelectFilterBaseT extends FilterBaseT {
  unit?: string;
  options: SelectOptionT[];
  defaultValue: MultiSelectFilterValueT | null;
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
  | RangeFilterT;

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
  type: // TODO: NOT USED, the type is clear based on the filter id
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
    | MultiSelectFilterValueT;
}

export interface TableConfigT {
  id: TableIdT;
  filters?: FilterConfigT;
}

export interface QueryConceptT {
  type: "CONCEPT";
  ids: ConceptIdT[];
  label: string; // Used to expand
  excludeFromTimestampAggregation: boolean; // TODO: Not used
  tables: TableConfigT[];
  selects?: SelectorIdT[];
}

export type QueryIdT = string;
export interface SavedQueryT {
  type: "SAVED_QUERY";
  query: QueryIdT; // TODO: rename this "id"
}

export interface OrQueryT {
  type: "OR";
  children: (QueryConceptT | SavedQueryT)[];
}

export interface DateRestrictionQueryT {
  type: "DATE_RESTRICTION";
  dateRange: DateRangeT;
  child: OrQueryT;
}

export interface NegationQueryT {
  type: "NEGATION";
  child: DateRestrictionQueryT | OrQueryT;
}

export interface AndQueryT {
  type: "AND";
  children: (DateRestrictionQueryT | NegationQueryT | OrQueryT)[];
}

export interface QueryT {
  type: "CONCEPT_QUERY";
  root: AndQueryT | NegationQueryT | DateRestrictionQueryT;
}

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

// TODO: This actually returns GETStoredQueryResponseT => a lot of unused fields
export interface PostQueriesResponseT {
  id: QueryIdT;
}

export type ColumnDescriptionKind =
  | "ID"
  | "STRING"
  | "INTEGER"
  | "MONEY"
  | "NUMERIC"
  | "DATE"
  | "DATE_RANGE"
  | "BOOLEAN"
  | "CATEGORICAL"
  | "RESOLUTION";

export interface ColumnDescription {
  label: string;
  selectId: string | null;
  type: ColumnDescriptionKind;
}

// TODO: This actually returns GETStoredQueryResponseT => a lot of unused fields
export interface GetQueryResponseDoneT {
  status: "DONE";
  numberOfResults: number;
  resultUrl: string;
  columnDescriptions: ColumnDescription[];
  queryType: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
}

// TODO: This actually returns GETStoredQueryResponseT => a lot of unused fields
export interface GetQueryErrorResponseT {
  status: "FAILED" | "CANCELED";
  error: ErrorResponseT | null;
}

export interface ErrorResponseT {
  id?: string;
  code: SupportedErrorCodesT; // To translate to localized messages
  message?: string; // For developers / debugging only
  context?: Record<string, string>; // More information to maybe display in translated messages
}

export type GetQueryResponseT = GetQueryResponseDoneT | GetQueryErrorResponseT;

export interface GetStoredQueryResponseT {
  id: QueryIdT;
  label: string;
  createdAt: string; // ISO timestamp: 2019-06-18T11:11:50.528626+02:00
  own: boolean;
  shared: boolean;
  system: boolean;
  ownerName: string;
  numberOfResults: number;
  resultUrl: string;
  requiredTime: number; // TODO: Not used
  tags?: string[];
  query: QueryT;
  queryType: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
  secondaryId: string | null;
  owner: string; // TODO: Remove. Not used. And it's actually an ID
  status: "DONE" | "NEW"; // TODO: Remove. Not used here
  groups?: UserGroupIdT[];
  canExpand?: boolean;
  availableSecondaryIds?: string[];
}

// TODO: This actually returns a lot of unused fields, see above
// TODO: But actually, it's not correct, because some fields are not
//       returned on the LIST response, which ARE returned in the
//       single result response
export type GetStoredQueriesResponseT = GetStoredQueryResponseT[];

export interface PostConceptResolveResponseT {
  resolvedConcepts?: string[];
  unknownCodes?: string[]; // TODO: Use "unknownConcepts"
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
  templateValues: string[]; // unclear whether that's correct
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
