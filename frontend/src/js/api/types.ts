// This file specifies
// - response type provided by the backend API
// - partial types that the reponses are built from

import type { Forms } from "./form-types";

export type DatasetIdT = string;
export type DatasetT = {
  id: DatasetIdT,
  label: string
};

export type SelectOptionT = {
  label: string,
  value: number | string
};

export type SelectOptionsT = SelectOptionT[];

// Example: { min: "2019-01-01", max: "2019-12-31" }
export type DateRangeT = ?{ min?: string, max?: string };

export type CurrencyConfigT = {
  prefix: string,
  thousandSeparator: string,
  decimalSeparator: string,
  decimalScale: number
};

export type FilterIdT = string;
export type FilterBaseT = {
  id: FilterIdT,
  label: string,
  description?: string
};

export type RangeFilterValueT = {
  min?: number,
  max?: number,
  exact?: number
};
export type RangeFilterT = FilterBaseT & {
  type: "INTEGER_RANGE" | "REAL_RANGE" | "MONEY_RANGE",
  value: ?RangeFilterValueT,
  unit?: string,
  mode: "range" | "exact",
  precision?: number,
  min?: number,
  max?: number,
  pattern?: string
};

export type MultiSelectFilterValueT = (string | number)[];
export type MultiSelectFilterT = FilterBaseT & {
  type: "MULTI_SELECT",
  unit?: string,
  options: SelectOptionsT,
  defaultValue: ?MultiSelectFilterValueT
};

export type BigMultiSelectFilterT = MultiSelectFilterT & {
  type: "BIG_MULTI_SELECT",
  allowDropFile: boolean,
  // Not needed in this format:
  template: {
    filePath: string, // "/.../import/stable/Referenzen/example.csv",
    columns: string[],
    columnValue: string, // Unclear, what that's even needed for
    value: string,
    optionValue: string
  }
};

export type SelectFilterValueT = string | number;
export type SelectFilterT = FilterBaseT & {
  type: "SELECT",
  unit?: string,
  options: SelectOptionsT,
  defaultValue: ?SelectFilterValueT
};

export type StringFilterValueT = string;
export type StringFilterT = FilterBaseT & {
  type: "STRING"
};

export type DateColumnT = {
  options: SelectOptionsT,
  defaultValue: ?string,
  value?: string
};

export type FilterT =
  | StringFilterT
  | SelectFilterT
  | MultiSelectFilterT
  | RangeFilterT;

export type TableIdT = string;
export type TableT = {
  id: TableIdT,
  dateColumn: ?DateColumnT,
  connectorId: string, // TODO: Get rid of two ids here (unclear when which one should be used)
  label: string,
  exclude?: boolean,
  filters?: FilterT[], // Empty array: key not defined
  selects?: SelectorT[] // Empty array: key not defined
};

export type SelectorIdT = string;
export type SelectorT = {
  id: SelectorIdT,
  label: string,
  description: string,
  default?: boolean
};

export type InfoT = {
  key: string,
  value: string
};

export type ConceptIdT = string;

export type ConceptBaseT = {
  label: string,
  active: boolean,
  detailsAvailable: boolean,
  codeListResolvable: boolean,
  matchingEntries: number, // TODO: Don't send with struct nodes (even sent with 0)
  children?: ConceptIdT[], // Might be an empty struct or a "virtual node"
  description?: string, // Empty array: key not defined
  additionalInfos?: InfoT[], // Empty array: key not defined
  dateRange?: DateRangeT
};

export type ConceptStructT = ConceptBaseT;

export type ConceptElementT = ConceptBaseT & {
  parent?: ConceptIdT, // If not set, it's nested under a struct node
  tables?: TableT[], // Empty array: key not defined
  selects?: SelectorT[] // Empty array: key not defined
};

export type ConceptT = ConceptElementT | ConceptStructT;

export type FilterConfigT = {
  filter: FilterIdT, // TODO: Rename this: "id"
  type:  // TODO: NOT USED, the type is clear based on the filter id
    | "INTEGER_RANGE"
    | "REAL_RANGE"
    | "MONEY_RANGE"
    | "STRING"
    | "SELECT"
    | "MULTI_SELECT"
    | "BIG_MULTI_SELECT",
  value:
    | StringFilterValueT
    | RangeFilterValueT
    | SelectFilterValueT
    | MultiSelectFilterValueT
    | BigMultiSelectFilterValueT
};

export type TableConfigT = {
  id: TableIdT,
  filters?: FilterConfigT
}[];

export type SelectsConfigT = SelectorIdT[];

export type QueryConceptT = {
  type: "CONCEPT",
  ids: ConceptIdT[],
  label: string, // Used to expand
  excludeFromTimestampAggregation: boolean, // TODO: Not used
  tables: TableConfigT,
  selects?: SelectsConfigT
};

export type QueryIdT = string;
export type SavedQueryT = {
  type: "SAVED_QUERY",
  query: QueryIdT // TODO: rename this "id"
};

export type OrQueryT = {
  type: "OR",
  children: (QueryConceptT | SavedQueryT)[]
};

export type DateRestrictionQueryT = {
  type: "DATE_RESTRICTION",
  dateRange: DateRangeT,
  child: OrQueryT
};

export type NegationQueryT = {
  type: "NEGATION",
  child: DateRestrictionQueryT | OrQueryT
};

export type AndQueryT = {
  type: "AND",
  children: (DateRestrictionQueryT | NegationQueryT | OrQueryT)[]
};

export type QueryT = {
  type: "CONCEPT_QUERY",
  root: AndQueryT | NegationQueryT | DateRestrictionQueryT
};

// ---------------------------------------
// ---------------------------------------
// API RESPONSES
// ---------------------------------------
// ---------------------------------------
export type GetDatasetsResponseT = DatasetT[];

export type GetFrontendConfigResponseT = {
  currency: CurrencyConfigT,
  version: string
};

export type GetConceptResponseT = {
  [key: ConceptIdT]: ConceptElementT
};

export type GetConceptsResponseT = {
  concepts: {
    [key: ConceptIdT]: ConceptStructT | ConceptElementT
  },
  version?: number // TODO: Is this even sent anymore?
};

// TODO: This actually returns GETStoredQueryResponseT => a lot of unused fields
export type PostQueriesResponseT = {
  id: QueryIdT
};

// TODO: This actually returns GETStoredQueryResponseT => a lot of unused fields
export type GetQueryResponseDoneT = {
  status: "DONE",
  numberOfResults: number,
  resultUrl: string
};

export type GetQueryResponseT =
  | GetQueryResponseDoneT
  | {
      status: "FAILED" | "CANCELED"
    };

export type GetStoredQueryResponseT = {
  id: QueryIdT,
  label: string,
  createdAt: string, // ISO timestamp: 2019-06-18T11:11:50.528626+02:00
  own: boolean,
  shared: boolean,
  system: boolean,
  ownerName: string,
  numberOfResults: number,
  resultUrl: string,
  requiredTime: number, // TODO: Not used
  tags?: string[],
  query: QueryT, // TODO: Remove in QUERIES response. Creates a lot of additional traffic right now
  owner: string, // TODO: Remove. Not used. And it's actually an ID
  status: "DONE" // TODO: Remove. Not used here
};

// TODO: This actually returns a lot of unused fields, see above
export type GetStoredQueriesResponseT = GetStoredQueryResponseT[];

export type PostConceptResolveResponseT = {
  resolvedConcepts?: string[],
  unknownCodes?: string[] // TODO: Use "unknownConcepts"
};

export type PostFilterResolveResponseT = {
  unknownCodes?: string[],
  resolvedFilter?: {
    filterId: FilterIdT,
    tableId: TableIdT,
    value: {
      label: string,
      value: string
    }[]
  }
};

export type PostFilterSuggestionsResponseT = {
  label: string,
  value: string,
  optionValue: ?string,
  templateValues: string[] // unclear whether that's correct
}[];

export type GetFormQueriesResponseT = Forms;

export type Permission = {
  domains: string[],
  abilities: string[],
  targets: string[]
};

export type UserGroup = {
  groupId: string,
  label: string
};

export type GetMeResponseT = {
  userName: string,
  permissions: Permission[],
  groups: UserGroup[]
};

export type PostLoginResponseT = {
  access_token: string
};
