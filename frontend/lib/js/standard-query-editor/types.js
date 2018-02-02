// @flow
import { type SelectOptionsType } from '../common/types';

type RangeFilterValueType = { min?: number, max?: number, exact?: number }
export type RangeFilterType = {
  id: number,
  label: string,
  description?: string,
  type: 'INTEGER_RANGE' | 'REAL_RANGE',
  value: ?RangeFilterValueType,
  unit?: string,
  mode: 'range' | 'exact',
  precision?: number,
  min?: number,
  max?: number,
}

type MultiSelectFilterValueType = (string | number)[];
export type MultiSelectFilterType = {
  id: number,
  label: string,
  description?: string,
  type: 'MULTI_SELECT',
  value: ?MultiSelectFilterValueType,
  unit?: string,
  options: SelectOptionsType
}

type SelectFilterValueType = string | number;
export type SelectFilterType = {
  id: number,
  label: string,
  description?: string,
  type: 'SELECT',
  value: ?SelectFilterValueType,
  unit?: string,
  options: SelectOptionsType
}

export type FilterType = SelectFilterType | MultiSelectFilterType | RangeFilterType;

export type TableType = {
  id: number,
  label: string,
  exclude?: boolean,
  filters: ?FilterType[],
};

export type ElementType = {
  id: number,
  label: string,
  description: string,
  tables: TableType[],
  additionalInfos: [],
  matchingEntries: number,
  isPreviousQuery: boolean,
  hasActiveFilters: boolean,
  excludeTimestamps: boolean,
  // eslint-disable-next-line no-use-before-define
  query?: PreviousQuery,
  loading?: boolean,
  error?: string,
};

export type QueryGroupType = {
  elements: ElementType[],
  dateRange?: ?{ min?: string, max?: string },
};

type PreviousQuery = {
  groups: QueryGroupType[]
};


export type StandardQueryType = QueryGroupType[];
