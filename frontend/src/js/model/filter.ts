import type {
  BigMultiSelectFilterT,
  FilterT,
  MultiSelectFilterT,
} from "../api/types";
import { exists } from "../common/helpers/exists";

import { NodeResetConfig } from "./node";

const resetFilter =
  (config: NodeResetConfig = {}) =>
  (filter: FilterT) => {
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
      default:
        return {
          ...filter,
          value: config.useDefaults ? filter.defaultValue || null : null,
        };
    }
  };

export const resetFilters = (
  filters?: FilterT[],
  config: NodeResetConfig = {},
) => (filters ? filters.map(resetFilter(config)) : []);

export const isMultiSelectFilter = (
  filter: FilterT,
): filter is BigMultiSelectFilterT | MultiSelectFilterT =>
  filter.type === "MULTI_SELECT" || filter.type === "BIG_MULTI_SELECT";
