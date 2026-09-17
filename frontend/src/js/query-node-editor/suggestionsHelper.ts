import type { FilterSuggestion, SelectOptionT } from "../api/types";

export const filterSuggestionToSelectOption = ({
  value,
  label,
  disabled,
}: FilterSuggestion): SelectOptionT => ({ value, label, disabled });
