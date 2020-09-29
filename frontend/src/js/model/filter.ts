import { MULTI_SELECT, BIG_MULTI_SELECT } from "../form-components/filterTypes";

const filterWithDefaults = filter => {
  switch (filter.type) {
    case MULTI_SELECT:
    case BIG_MULTI_SELECT:
      return {
        ...filter,
        value: filter.defaultValue || []
      };
    default:
      return {
        ...filter,
        value: filter.defaultValue || null
      };
  }
};

export const filtersWithDefaults = filters =>
  filters ? filters.map(filterWithDefaults) : null;
