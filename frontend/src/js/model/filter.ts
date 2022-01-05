import type {
  BigMultiSelectFilterT,
  FilterT,
  MultiSelectFilterT,
} from "../api/types";
import { exists } from "../common/helpers/exists";
import type { FilterWithValueType } from "../standard-query-editor/types";

import { NodeResetConfig } from "./node";

const resetFilter =
  (config: NodeResetConfig = {}) =>
  (filter: FilterWithValueType): FilterWithValueType => {
    switch (filter.type) {
      case "MULTI_SELECT":
      case "BIG_MULTI_SELECT":
        return {
          ...filter,
          value:
            config.useDefaults && filter.defaultValue
              ? filter.defaultValue
                  .map((val) => filter.options.find((opt) => opt.value === val))
                  .filter(exists)
              : [],
        };
      case "SELECT":
        return {
          ...filter,
          value: config.useDefaults
            ? filter.defaultValue || null
            : filter.options.length > 0
            ? filter.options[0].value
            : null,
        };
      default:
        return {
          ...filter,
          value: config.useDefaults ? filter.defaultValue || null : null,
        };
    }
  };

const filterHasValue = (filter: FilterWithValueType) => {
  switch (filter.type) {
    case "MULTI_SELECT":
    case "BIG_MULTI_SELECT":
      return filter.value && filter.value.length > 0;
    case "SELECT":
      return (
        filter.value &&
        filter.options.length > 0 &&
        filter.value !== filter.options[0].value
      );
    default:
      return exists(filter.value);
  }
};

export const filtersHaveValues = (filters: FilterWithValueType[]) =>
  filters.some(filterHasValue);

export const filterValueDiffersFromDefault = (
  filter: FilterWithValueType,
): boolean => {
  switch (filter.type) {
    // Since MULTI_SELECT & BIG_MULTI_SELECT's defaultValue and value have differnt shapes
    // we'll need to fully compare those
    case "MULTI_SELECT":
    case "BIG_MULTI_SELECT":
      const jsonifiedDefaultValue = JSON.stringify(filter.defaultValue || []);
      const jsonifiedValue = JSON.stringify(
        (filter.value || [])?.map((o) => o.value) || [],
      );

      return jsonifiedDefaultValue !== jsonifiedValue;
    default:
      return (
        JSON.stringify(filter.value) !== JSON.stringify(filter.defaultValue)
      );
  }
};

export const resetFilters = (
  filters?: FilterWithValueType[],
  config: NodeResetConfig = {},
) => (filters ? filters.map(resetFilter(config)) : []);

export const isMultiSelectFilter = (
  filter: FilterT,
): filter is BigMultiSelectFilterT | MultiSelectFilterT =>
  filter.type === "MULTI_SELECT" || filter.type === "BIG_MULTI_SELECT";

export const filterIsEmpty = (filter: FilterWithValueType) => {
  switch (filter.type) {
    case "BIG_MULTI_SELECT":
    case "MULTI_SELECT":
      return !filter.value || filter.value.length === 0;
    case "SELECT":
      return (
        !filter.value ||
        (filter.options.length > 0 && filter.value === filter.options[0].value)
      );
    default:
      return !filter.value;
  }
};
