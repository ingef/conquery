import type { FilterSuggestion, SelectOptionT } from "../../api/types";

export const isFilterSuggestion = (
  option: SelectOptionT | FilterSuggestion,
): option is FilterSuggestion => {
  return "optionValue" in option && "templateValues" in option;
};
