import type {
  BigMultiSelectFilterT,
  FilterT,
  MultiSelectFilterT,
} from "../api/types";

const filterWithDefaults = (filter: FilterT) => {
  switch (filter.type) {
    case "MULTI_SELECT":
    case "BIG_MULTI_SELECT":
      return {
        ...filter,
        value: filter.defaultValue || [],
      };
    default:
      return {
        ...filter,
        value: filter.defaultValue || null,
      };
  }
};

export const filtersWithDefaults = (filters?: FilterT[]) =>
  filters ? filters.map(filterWithDefaults) : [];

export const isMultiSelectFilter = (
  filter: FilterT,
): filter is BigMultiSelectFilterT | MultiSelectFilterT =>
  filter.type === "MULTI_SELECT" || filter.type === "BIG_MULTI_SELECT";
